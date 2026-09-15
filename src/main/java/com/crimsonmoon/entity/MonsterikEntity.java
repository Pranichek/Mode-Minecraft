package com.crimsonmoon.entity;

import com.crimsonmoon.world.MonsterikBossManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MonsterikEntity extends HostileEntity implements GeoEntity {

    public static final double MAX_HEALTH = 400.0;

    private static final double PHASE_2_HEALTH_FRACTION = 0.66;
    private static final double PHASE_3_HEALTH_FRACTION = 0.33;

    private static final double BASE_MOVEMENT_SPEED = 0.34;
    private static final double BASE_ATTACK_DAMAGE = 7.0;
    private static final double BASE_ARMOR = 6.0;

    private static final double[] PHASE_MOVEMENT_SPEED = {BASE_MOVEMENT_SPEED, 0.40, 0.47};
    private static final double[] PHASE_ATTACK_DAMAGE = {BASE_ATTACK_DAMAGE, 10.0, 14.0};

    private static final double DETECTION_RANGE = 25.0;

    private static final int[] DASH_COOLDOWN_TICKS = {0, 16 * 20, 9 * 20}; 
    private static final double DASH_MIN_RANGE = 4.0;
    private static final double DASH_MAX_RANGE = 12.0;
    private static final int DASH_TRAVEL_TICKS = 14;
    private static final double DASH_HIT_RADIUS = 2.0;
    private static final double DASH_KNOCKBACK = 0.9;
    private static final float DASH_DAMAGE_BONUS = 4.0f;

    private static final int[] LIGHTNING_COOLDOWN_TICKS = {18 * 20, 14 * 20, 10 * 20};
    private static final double LIGHTNING_TRIGGER_RANGE = 22.0;   

    private static final int LIGHTNING_RING_COUNT = 3;            
    private static final int LIGHTNING_PER_RING = 8;
    private static final double LIGHTNING_RING_MIN_RADIUS = 3.0;      
    private static final double LIGHTNING_RING_WIDTH = 1.8;
    private static final double LIGHTNING_RING_SPACING = 4.5;         

    private static final double LIGHTNING_TELEGRAPH_RADIUS = 0.6;     
    private static final int LIGHTNING_RISE_TICKS = 18;               
    private static final double LIGHTNING_HIT_RADIUS = 1.4;
    private static final float LIGHTNING_DAMAGE = 12.0f;

    private static final int GLOBAL_ATTACK_DELAY_TICKS = 2 * 20; 

    private record WaveMobEntry(EntityType<? extends MobEntity> type, int count) {}

    private static final WaveMobEntry[][] SUMMON_WAVES = {
        { new WaveMobEntry(EntityType.ZOMBIE, 4) },
        { new WaveMobEntry(EntityType.SKELETON, 2), new WaveMobEntry(EntityType.HUSK, 2) },
        { new WaveMobEntry(EntityType.PILLAGER, 2), new WaveMobEntry(EntityType.VINDICATOR, 1) },
        { new WaveMobEntry(EntityType.VINDICATOR, 3), new WaveMobEntry(EntityType.EVOKER, 2) },
    };

    private static final double SUMMON_SPAWN_RADIUS = 5.0;

    private static final Vector3f ENRAGE_PARTICLE_COLOR = new Vector3f(0.85f, 0.1f, 0.1f);
    private static final Vector3f LIGHTNING_PARTICLE_COLOR = new Vector3f(0.2f, 0.85f, 0.78f); 

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossBar bossBar = new ServerBossBar(
        this.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.NOTCHED_10
    );

    private int phase = 1; 

    private int globalAttackDelay = 0;
    private int dashCooldown = DASH_COOLDOWN_TICKS[1];

    private boolean dashActive = false;
    private int dashProgress = 0;
    private double dashStartX, dashStartZ, dashTargetX, dashTargetZ;
    private boolean dashDamageDealt = false;

    private int lightningCooldown = LIGHTNING_COOLDOWN_TICKS[0];
    private boolean lightningAttackActive = false;
    private int lightningAttackProgress = 0;
    private boolean lightningStruck = false;
    private final List<double[]> lightningPositions = new ArrayList<>(); 

    private boolean summonEventUsed = false; 
    private boolean summonEventActive = false;
    private int summonWaveIndex = -1; 
    private final List<UUID> currentWaveMobUuids = new ArrayList<>();

    public MonsterikEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("SummonEventUsed", this.summonEventUsed);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.summonEventUsed = nbt.getBoolean("SummonEventUsed");
    }

    public static DefaultAttributeContainer.Builder createMonsterikAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
            .add(EntityAttributes.GENERIC_ARMOR, BASE_ARMOR)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, DETECTION_RANGE)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.bossBar.clearPlayers();
        discardRemainingWaveMobs();

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            
            // --- ЕПІЧНА СМЕРТЬ ТА ДРОП (БЕЗ ТЕЛЕПОРТАЦІЇ) ---
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 2, 0, 0, 0, 0);
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 1.0f, 1.0f);
            
            this.dropItem(com.crimsonmoon.ModItems.XRAY_GLASSES);
            
            com.crimsonmoon.world.CrimsonMoonWorldState state = com.crimsonmoon.world.CrimsonMoonWorldState.getServerState(serverWorld.getServer());
            state.monsterikDefeated = true;
            state.markDirty();
            // ------------------------------------------------

            MonsterikBossManager.onMonsterikDied(serverWorld, this);
        }
    }

    private void discardRemainingWaveMobs() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (UUID id : currentWaveMobUuids) {
            Entity entity = serverWorld.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
        currentWaveMobUuids.clear();
    }

    @Override
    public void tick() {
        super.tick();

        this.bossBar.setPercent(MathHelper.clamp(this.getHealth() / this.getMaxHealth(), 0.0f, 1.0f));

        if (this.getWorld().isClient) {
            return;
        }

        if (!this.isAlive()) {
            this.dashActive = false;
            this.lightningAttackActive = false;
            this.summonEventActive = false;
            return;
        }

        updatePhase();

        if (this.dashActive) {
            progressDash();
        }
        if (this.lightningAttackActive) {
            progressLightningRingAttack();
        }
        if (this.summonEventActive) {
            progressSummonEvent();
        }

        if (this.globalAttackDelay > 0) {
            this.globalAttackDelay--;
        }
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
        }
        if (this.lightningCooldown > 0) {
            this.lightningCooldown--;
        }

        if (this.summonEventActive) {
            return;
        }

        if (this.dashActive || this.lightningAttackActive || this.globalAttackDelay > 0) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        double distance = this.distanceTo(target);

        if (this.phase >= 2 && this.dashCooldown <= 0
                && distance >= DASH_MIN_RANGE && distance <= DASH_MAX_RANGE) {
            startDash(target);
            this.dashCooldown = DASH_COOLDOWN_TICKS[this.phase - 1];
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
            return;
        }

        if (this.lightningCooldown <= 0 && distance <= LIGHTNING_TRIGGER_RANGE) {
            startLightningRingAttack();
            this.lightningCooldown = LIGHTNING_COOLDOWN_TICKS[this.phase - 1];
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
        }
    }

    private void updatePhase() {
        double healthFraction = this.getHealth() / this.getMaxHealth();

        int newPhase;
        if (healthFraction <= PHASE_3_HEALTH_FRACTION) {
            newPhase = 3;
        } else if (healthFraction <= PHASE_2_HEALTH_FRACTION) {
            newPhase = 2;
        } else {
            newPhase = 1;
        }

        if (newPhase == this.phase) {
            return;
        }

        this.phase = newPhase;
        applyPhaseStats(newPhase);
        playPhaseTransitionEffects(newPhase);

        if (newPhase == 3 && !this.summonEventUsed) {
            this.summonEventUsed = true;
            startSummonEvent();
        }
    }

    private void applyPhaseStats(int newPhase) {
        int index = newPhase - 1;

        EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(PHASE_MOVEMENT_SPEED[index]);
        }

        EntityAttributeInstance damage = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(PHASE_ATTACK_DAMAGE[index]);
        }

        if (newPhase >= 2) {
            this.dashCooldown = Math.min(this.dashCooldown, DASH_COOLDOWN_TICKS[index] / 2);
        }
    }

    private void playPhaseTransitionEffects(int newPhase) {
        if (newPhase == 2) {
            this.bossBar.setColor(BossBar.Color.YELLOW);
        } else if (newPhase == 3) {
            this.bossBar.setColor(BossBar.Color.RED);
        }

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            newPhase == 3 ? SoundEvents.ENTITY_ENDER_DRAGON_GROWL : SoundEvents.ENTITY_RAVAGER_ROAR,
            SoundCategory.HOSTILE,
            1.3f, newPhase == 3 ? 0.7f : 0.9f
        );

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            DustParticleEffect enrageDust = new DustParticleEffect(ENRAGE_PARTICLE_COLOR, 1.4f);
            serverWorld.spawnParticles(
                enrageDust, this.getX(), this.getY() + 1.2, this.getZ(),
                60, 0.6, 0.9, 0.6, 0.02
            );
            serverWorld.spawnParticles(
                ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.8, this.getZ(),
                8, 0.4, 0.4, 0.4, 0.0
            );
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (!this.isAlive() || this.summonEventActive) {
            return false;
        }
        boolean result = super.tryAttack(target);
        if (result) {
            this.triggerAnim("main_controller", "attack");
        }
        return result;
    }

    private void startDash(LivingEntity target) {
        this.dashStartX = this.getX();
        this.dashStartZ = this.getZ();
        this.dashTargetX = target.getX();
        this.dashTargetZ = target.getZ();

        this.dashActive = true;
        this.dashProgress = 0;
        this.dashDamageDealt = false;

        this.getNavigation().stop();
        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_RAVAGER_ATTACK,
            SoundCategory.HOSTILE,
            1.1f, 1.0f
        );
    }

    private void progressDash() {
        this.dashProgress++;
        double t = Math.min(1.0, this.dashProgress / (double) DASH_TRAVEL_TICKS);
        double eased = 1 - Math.pow(1 - t, 2);

        double x = MathHelper.lerp(eased, this.dashStartX, this.dashTargetX);
        double z = MathHelper.lerp(eased, this.dashStartZ, this.dashTargetZ);
        this.setPosition(x, this.getY(), z);

        if (!this.dashDamageDealt && t >= 0.85 && this.getWorld() instanceof ServerWorld serverWorld) {
            dealDashDamage(serverWorld);
            this.dashDamageDealt = true;
        }

        if (t >= 1.0) {
            this.dashActive = false;
        }
    }

    private void dealDashDamage(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }
            if (player.squaredDistanceTo(this) <= DASH_HIT_RADIUS * DASH_HIT_RADIUS) {
                float damage = (float) PHASE_ATTACK_DAMAGE[this.phase - 1] + DASH_DAMAGE_BONUS;
                player.damage(this.getDamageSources().mobAttack(this), damage);

                double dx = player.getX() - this.getX();
                double dz = player.getZ() - this.getZ();
                double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                player.addVelocity(dx / len * DASH_KNOCKBACK, 0.4, dz / len * DASH_KNOCKBACK);
                player.velocityModified = true;
            }
        }
    }

    private void startSummonEvent() {
        this.summonEventActive = true;
        this.summonWaveIndex = -1;
        this.currentWaveMobUuids.clear();

        this.dashActive = false;
        this.lightningAttackActive = false;

        BlockPos spawnPos = MonsterikBossManager.ARENA_SPAWN_POS;
        this.requestTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        this.setInvulnerable(true);
        this.setAiDisabled(true); 
        this.getNavigation().stop();
        this.setTarget(null);
        this.setVelocity(0.0, this.getVelocity().y, 0.0);
        this.velocityModified = true;

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.HOSTILE,
            1.3f, 0.8f
        );

        spawnNextWave();
    }

    private void progressSummonEvent() {
        this.getNavigation().stop();
        this.setVelocity(0.0, this.getVelocity().y, 0.0);
        this.velocityModified = true;

        if (this.summonWaveIndex < 0 || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        for (UUID id : this.currentWaveMobUuids) {
            Entity entity = serverWorld.getEntity(id);
            if (entity != null && entity.isAlive()) {
                return; 
            }
        }

        spawnNextWave();
    }

    private void spawnNextWave() {
        this.summonWaveIndex++;
        this.currentWaveMobUuids.clear();

        if (this.summonWaveIndex >= SUMMON_WAVES.length) {
            endSummonEvent();
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        WaveMobEntry[] wave = SUMMON_WAVES[this.summonWaveIndex];
        int totalMobs = 0;
        for (WaveMobEntry entry : wave) {
            totalMobs += entry.count();
        }

        int spawnedSoFar = 0;
        for (WaveMobEntry entry : wave) {
            for (int i = 0; i < entry.count(); i++) {
                MobEntity mob = spawnWaveMob(serverWorld, entry.type(), spawnedSoFar, totalMobs);
                if (mob != null) {
                    this.currentWaveMobUuids.add(mob.getUuid());
                }
                spawnedSoFar++;
            }
        }

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON,
            SoundCategory.HOSTILE,
            1.2f, 0.9f
        );
    }

    private MobEntity spawnWaveMob(ServerWorld world, EntityType<? extends MobEntity> type, int index, int total) {
        MobEntity mob = type.create(world);
        if (mob == null) {
            return null;
        }

        double angle = (Math.PI * 2 / Math.max(1, total)) * index;
        double x = this.getX() + Math.cos(angle) * SUMMON_SPAWN_RADIUS;
        double z = this.getZ() + Math.sin(angle) * SUMMON_SPAWN_RADIUS;
        double groundY = findGroundY(x, z);

        mob.refreshPositionAndAngles(x, groundY, z, this.random.nextFloat() * 360.0f, 0.0f);

        if (mob instanceof PillagerEntity) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (mob instanceof VindicatorEntity) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else if (mob instanceof SkeletonEntity) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        mob.setPersistent();

        LivingEntity summonTarget = this.getAttacker() != null ? this.getAttacker() : findNearestPlayer(world);
        if (summonTarget != null) {
            mob.setTarget(summonTarget);
        }

        world.spawnEntity(mob);
        return mob;
    }

    private PlayerEntity findNearestPlayer(ServerWorld world) {
        return world.getClosestPlayer(this, LIGHTNING_TRIGGER_RANGE * 2.0);
    }

    private void endSummonEvent() {
        this.summonEventActive = false;
        this.summonWaveIndex = -1;
        this.currentWaveMobUuids.clear();
        this.setInvulnerable(false);
        this.setAiDisabled(false); 

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_RAVAGER_ROAR,
            SoundCategory.HOSTILE,
            1.2f, 1.0f
        );
    }

    private void startLightningRingAttack() {
        lightningPositions.clear();

        double baseAngle = this.random.nextDouble() * Math.PI * 2;
        double angleStep = (Math.PI * 2) / LIGHTNING_PER_RING;

        for (int ring = 0; ring < LIGHTNING_RING_COUNT; ring++) {
            double ringAngleOffset = baseAngle + (angleStep / 2.0) * ring;
            double ringMinRadius = LIGHTNING_RING_MIN_RADIUS + ring * LIGHTNING_RING_SPACING;
            double ringMaxRadius = ringMinRadius + LIGHTNING_RING_WIDTH;

            for (int i = 0; i < LIGHTNING_PER_RING; i++) {
                double angle = ringAngleOffset + angleStep * i + (this.random.nextDouble() - 0.5) * angleStep * 0.3;
                double dist = ringMinRadius + this.random.nextDouble() * (ringMaxRadius - ringMinRadius);

                double x = this.getX() + Math.cos(angle) * dist;
                double z = this.getZ() + Math.sin(angle) * dist;
                double groundY = findGroundY(x, z);

                lightningPositions.add(new double[]{x, groundY, z});
            }
        }

        lightningAttackActive = true;
        lightningAttackProgress = 0;
        lightningStruck = false;

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_EVOKER_CAST_SPELL,
            SoundCategory.HOSTILE,
            1.2f, 0.6f
        );
    }

    private void progressLightningRingAttack() {
        lightningAttackProgress++;
        ServerWorld world = (ServerWorld) this.getWorld();

        for (double[] pos : lightningPositions) {
            spawnLightningTelegraphRing(world, pos[0], pos[1], pos[2]);
        }

        if (!lightningStruck && lightningAttackProgress >= LIGHTNING_RISE_TICKS) {
            strikeLightningAtAllPositions(world);
            lightningStruck = true;
        }

        if (lightningAttackProgress >= LIGHTNING_RISE_TICKS + 2) {
            lightningAttackActive = false;
        }
    }

    private void spawnLightningTelegraphRing(ServerWorld world, double x, double groundY, double z) {
        DustParticleEffect turquoiseDust = new DustParticleEffect(LIGHTNING_PARTICLE_COLOR, 1.2f);

        int points = 10;
        for (int p = 0; p < points; p++) {
            double angle = (Math.PI * 2 / points) * p;
            double px = x + Math.cos(angle) * LIGHTNING_TELEGRAPH_RADIUS;
            double pz = z + Math.sin(angle) * LIGHTNING_TELEGRAPH_RADIUS;
            world.spawnParticles(turquoiseDust, px, groundY + 0.1, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void strikeLightningAtAllPositions(ServerWorld world) {
        for (double[] pos : lightningPositions) {
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt == null) {
                continue;
            }
            bolt.refreshPositionAfterTeleport(pos[0], pos[1], pos[2]);
            world.spawnEntity(bolt);

            dealLightningDamage(world, pos[0], pos[1], pos[2]);
        }
    }

    private void dealLightningDamage(ServerWorld world, double x, double groundY, double z) {
        Box hitBox = new Box(
            x - LIGHTNING_HIT_RADIUS, groundY - 0.5, z - LIGHTNING_HIT_RADIUS,
            x + LIGHTNING_HIT_RADIUS, groundY + 2.5, z + LIGHTNING_HIT_RADIUS
        );

        List<PlayerEntity> hitPlayers = world.getEntitiesByClass(PlayerEntity.class, hitBox, PlayerEntity::isAlive);
        for (PlayerEntity player : hitPlayers) {
            player.damage(this.getDamageSources().mobAttack(this), LIGHTNING_DAMAGE);
        }
    }

    private double findGroundY(double x, double z) {
        ServerWorld world = (ServerWorld) this.getWorld();
        BlockPos.Mutable pos = new BlockPos.Mutable(
            MathHelper.floor(x), MathHelper.floor(this.getY()) + 2, MathHelper.floor(z)
        );

        for (int i = 0; i < 6; i++) {
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return pos.getY() + 1.0;
            }
            pos.move(Direction.DOWN);
        }
        return this.getY();
    }

    @Override
    public net.minecraft.sound.SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    public net.minecraft.sound.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    public net.minecraft.sound.SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::movementPredicate)
            .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.monsterik.attack")));
    }

    private PlayState movementPredicate(AnimationState<MonsterikEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (this.dashActive) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.monsterik.run"));
        } else if (state.isMoving() && this.phase >= 2) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.monsterik.run"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}