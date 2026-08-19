package com.crimsonmoon.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
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

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Атака 1 (призыв): раз в SUMMON_COOLDOWN_TICKS призывает миньонов,
 * вокруг заспавненного миньона появляются зелёные частицы.
 *
 * Атака 2 (шипы): раз в SPIKE_ATTACK_COOLDOWN_TICKS выпускает вокруг
 * себя SPIKE_COUNT шипов из зелёных частиц — толстые у основания,
 * сужающиеся к верхушке, высотой SPIKE_HEIGHT блоков. Наносят урон
 * игроку, если тот стоит рядом с шипом в момент, когда шип
 * практически достиг полной высоты.
 */
public class ThorncallerEntity extends ZombieEntity implements GeoEntity {

    // === Настройка призыва подкрепления (атака 1) ===
    public static EntityType<? extends MobEntity> MINION_TYPE = EntityType.ZOMBIE;

    private static final String MINION_TAG = "thorncaller_minion";
    private static final int SUMMON_COOLDOWN_TICKS = 15 * 20; // раз в 15 секунд
    private static final int MINIONS_PER_CAST = 2;
    private static final int MAX_ALIVE_MINIONS = 6;   // сколько живых миньонов допускается рядом
    private static final double SUMMON_SEARCH_RADIUS = 3.0;
    private static final double MINION_TRACK_RADIUS = 24.0;

    // === Настройка атаки шипами (атака 2) ===
    private static final int SPIKE_ATTACK_COOLDOWN_TICKS = 12 * 20; // раз в 12 секунд
    private static final double SPIKE_TRIGGER_RANGE = 10.0;         // применяется только если цель не дальше этого
    private static final int SPIKE_COUNT = 6;                       // сколько шипов вокруг себя
    private static final double SPIKE_MIN_RADIUS = 2.0;             // минимальное расстояние шипа от себя
    private static final double SPIKE_MAX_RADIUS = 3.5;             // максимальное расстояние шипа от себя
    private static final double SPIKE_HEIGHT = 1.5;                 // высота шипа в блоках
    private static final double SPIKE_BASE_RADIUS = 0.35;           // толщина у основания
    private static final double SPIKE_TIP_RADIUS = 0.03;            // толщина на верхушке
    private static final int SPIKE_RISE_TICKS = 12;                 // сколько тиков шип растёт до полной высоты
    private static final int SPIKE_LINGER_TICKS = 10;               // сколько ещё держится на полной высоте
    private static final double SPIKE_HIT_RADIUS = 0.9;             // радиус, в котором шип наносит урон
    private static final float SPIKE_DAMAGE = 10.0f;

    private static final Vector3f THORN_PARTICLE_COLOR = new Vector3f(0.16f, 0.85f, 0.25f);

    private int summonCooldown = 60;
    private int spikeAttackCooldown = 100;

    private boolean spikeAttackActive = false;
    private int spikeAttackProgress = 0;
    private final List<double[]> spikePositions = new ArrayList<>();
    private boolean[] spikeDamageDealt = new boolean[0];

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ThorncallerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createThorncallerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.GENERIC_ARMOR, 1.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) {
            return;
        }

        if (this.spikeAttackActive) {
            progressSpikeAttack();
        }

        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }
        
        if (this.spikeAttackCooldown > 0) {
            this.spikeAttackCooldown--;
        }

        if (this.getTarget() == null) {
            return;
        }

        if (this.summonCooldown <= 0) {
            summonMinions();
            this.summonCooldown = SUMMON_COOLDOWN_TICKS;
            return; // не начинаем сразу вторую атаку в тот же тик
        }

        if (!this.spikeAttackActive && this.spikeAttackCooldown <= 0
            && this.distanceTo(this.getTarget()) <= SPIKE_TRIGGER_RANGE) {
            startSpikeAttack();
            this.spikeAttackCooldown = SPIKE_ATTACK_COOLDOWN_TICKS;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (result) {
            this.triggerAnim("main_controller", "attack");
        }
        return result;
    }

    // ==================== Атака 1: призыв миньонов ====================

    private void summonMinions() {
        ServerWorld world = (ServerWorld) this.getWorld();

        long aliveMinionsNearby = world.getEntitiesByClass(
            MobEntity.class,
            this.getBoundingBox().expand(MINION_TRACK_RADIUS),
            e -> e.isAlive() && e.getCommandTags().contains(MINION_TAG)
        ).size();

        if (aliveMinionsNearby >= MAX_ALIVE_MINIONS) {
            return;
        }

        int toSpawn = (int) Math.min(MINIONS_PER_CAST, MAX_ALIVE_MINIONS - aliveMinionsNearby);
        if (toSpawn <= 0) {
            return;
        }

        this.triggerAnim("main_controller", "attack");

        for (int i = 0; i < toSpawn; i++) {
            spawnOneMinion(world);
        }
    }

    private void spawnOneMinion(ServerWorld world) {
        MobEntity minion = MINION_TYPE.create(world);
        if (minion == null) {
            return;
        }

        BlockPos pos = findMinionSpawnPos(world);

        minion.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.getYaw(), 0.0f);
        minion.getCommandTags().add(MINION_TAG);

        world.spawnEntity(minion);

        if (minion instanceof HostileEntity hostile && this.getTarget() != null) {
            hostile.setTarget(this.getTarget());
        }

        spawnSummonParticles(world, minion.getX(), minion.getY(), minion.getZ());
    }

    private void spawnSummonParticles(ServerWorld world, double x, double y, double z) {
        DustParticleEffect thornDust = new DustParticleEffect(THORN_PARTICLE_COLOR, 1.1f);
        world.spawnParticles(thornDust, x, y + 1.0, z, 25, 0.35, 0.6, 0.35, 0.01);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 10, 0.35, 0.6, 0.35, 0.0);
    }

    private BlockPos findMinionSpawnPos(ServerWorld world) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.5 + this.random.nextDouble() * SUMMON_SEARCH_RADIUS;

            int x = MathHelper.floor(this.getX() + Math.cos(angle) * dist);
            int z = MathHelper.floor(this.getZ() + Math.sin(angle) * dist);
            int y = MathHelper.floor(this.getY());

            BlockPos candidate = new BlockPos(x, y, z);
            if (world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())
                && world.isSpaceEmpty(minionBox(candidate))) {
                return candidate;
            }
        }
        // не нашли идеальное место — призываем прямо рядом с собой
        return this.getBlockPos();
    }

    private Box minionBox(BlockPos pos) {
        return new Box(
            pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
            pos.getX() + 0.9, pos.getY() + 1.9, pos.getZ() + 0.9
        );
    }

    // ==================== Атака 2: шипы из земли ====================

    private void startSpikeAttack() {
        spikePositions.clear();

        double baseAngle = this.random.nextDouble() * Math.PI * 2;
        double angleStep = (Math.PI * 2) / SPIKE_COUNT;

        for (int i = 0; i < SPIKE_COUNT; i++) {
            // равномерно по кругу вокруг себя + небольшой случайный разброс,
            // чтобы шипы были рядом, но не залезали друг в друга
            double angle = baseAngle + angleStep * i + (this.random.nextDouble() - 0.5) * angleStep * 0.3;
            double dist = SPIKE_MIN_RADIUS + this.random.nextDouble() * (SPIKE_MAX_RADIUS - SPIKE_MIN_RADIUS);

            double x = this.getX() + Math.cos(angle) * dist;
            double z = this.getZ() + Math.sin(angle) * dist;
            double groundY = findSpikeGroundY(x, z);

            spikePositions.add(new double[]{x, groundY, z});
        }

        spikeDamageDealt = new boolean[spikePositions.size()];
        spikeAttackActive = true;
        spikeAttackProgress = 0;

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.BLOCK_HANGING_ROOTS_BREAK,
            SoundCategory.HOSTILE,
            1.2f, 0.7f
        );
    }

    private double findSpikeGroundY(double x, double z) {
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

    private void progressSpikeAttack() {
        spikeAttackProgress++;
        ServerWorld world = (ServerWorld) this.getWorld();

        double growFraction = Math.min(1.0, spikeAttackProgress / (double) SPIKE_RISE_TICKS);
        double currentHeight = SPIKE_HEIGHT * growFraction;

        for (int i = 0; i < spikePositions.size(); i++) {
            double[] spike = spikePositions.get(i);
            spawnSpikeParticles(world, spike[0], spike[1], spike[2], currentHeight);

            // урон наносится один раз, когда шип почти полностью вырос
            if (!spikeDamageDealt[i] && growFraction >= 0.75) {
                dealSpikeDamage(world, spike[0], spike[1], spike[2]);
                spikeDamageDealt[i] = true;
            }
        }

        if (spikeAttackProgress >= SPIKE_RISE_TICKS + SPIKE_LINGER_TICKS) {
            spikeAttackActive = false;
        }
    }

    private void spawnSpikeParticles(ServerWorld world, double x, double groundY, double z, double currentHeight) {
        if (currentHeight <= 0.0) {
            return;
        }

        DustParticleEffect thornDust = new DustParticleEffect(THORN_PARTICLE_COLOR, 1.3f);

        int rings = Math.max(2, (int) Math.ceil(currentHeight * 6));
        for (int r = 0; r <= rings; r++) {
            double t = r / (double) rings; // 0 у земли, 1 на верхушке шипа
            double y = groundY + currentHeight * t;
            // толстый у основания, сужается к верхушке
            double radius = MathHelper.lerp(t, SPIKE_BASE_RADIUS, SPIKE_TIP_RADIUS);

            int pointsInRing = radius > 0.15 ? 6 : 3;
            for (int p = 0; p < pointsInRing; p++) {
                double angle = (Math.PI * 2 / pointsInRing) * p;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                world.spawnParticles(thornDust, px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void dealSpikeDamage(ServerWorld world, double x, double groundY, double z) {
        Box hitBox = new Box(
            x - SPIKE_HIT_RADIUS, groundY - 0.5, z - SPIKE_HIT_RADIUS,
            x + SPIKE_HIT_RADIUS, groundY + SPIKE_HEIGHT + 0.5, z + SPIKE_HIT_RADIUS
        );

        List<PlayerEntity> hitPlayers = world.getEntitiesByClass(PlayerEntity.class, hitBox, PlayerEntity::isAlive);
        for (PlayerEntity player : hitPlayers) {
            player.damage(this.getDamageSources().mobAttack(this), SPIKE_DAMAGE);
            player.addVelocity(0.0, 0.35, 0.0);
            player.velocityModified = true;
        }
    }

    // ==================== Звуки ====================

    @Override
    public net.minecraft.sound.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public net.minecraft.sound.SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return null;
    }

    @Override
    public net.minecraft.sound.SoundEvent getDeathSound() {
        return null;
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::movementPredicate)
            .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.thorncaller.attack")));
    }

    private PlayState movementPredicate(AnimationState<ThorncallerEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.thorncaller.walk"));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.thorncaller.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}