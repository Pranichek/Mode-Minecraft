package com.crimsonmoon.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
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
import net.minecraft.entity.player.PlayerEntity;
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

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Босс Monsterik. 400 HP, полоса босса (boss bar) сверху экрана, 3 стадии
 * (фазы) по проценту здоровья, которые усиливают его на ходу:
 *
 *   Фаза 1 — 100%..66% HP: обычная скорость, размеренная ходьба ("walk"),
 *            только ближний бой.
 *   Фаза 2 —  66%..33% HP: ускоряется, периодически делает рывок ("run")
 *            в сторону цели с уроном при столкновении.
 *   Фаза 3 —  33%..0%  HP: "ярость" — ещё выше скорость и урон, рывок
 *            восстанавливается быстрее, красные частицы и рёв при входе
 *            в фазу.
 *
 * Единственность в мире (только один живой Monsterik одновременно) и спавн
 * на арене — обеспечиваются {@link com.crimsonmoon.world.MonsterikBossManager},
 * который также вызывается через {@code /monsterikboss spawn}
 * (см. {@link com.crimsonmoon.MonsterikBossCommand}).
 *
 * Модель/анимации грузятся через GeckoLib из:
 *   assets/crimsonmoon/geo/monsterik.geo.json
 *   assets/crimsonmoon/animations/monsterik.animation.json (walk / run / attack)
 *   assets/crimsonmoon/textures/entity/monsterik/monsterik.png
 * (см. MonsterikModel / MonsterikEntityRenderer).
 */
public class MonsterikEntity extends HostileEntity implements GeoEntity {

    // ==================== Общие константы ====================

    public static final double MAX_HEALTH = 400.0;

    private static final double PHASE_2_HEALTH_FRACTION = 0.66;
    private static final double PHASE_3_HEALTH_FRACTION = 0.33;

    // база (фаза 1) — дальше значения по фазам ниже
    // сделан заметно быстрее во всех фазах (было 0.24/0.28/0.33)
    private static final double BASE_MOVEMENT_SPEED = 0.34;
    private static final double BASE_ATTACK_DAMAGE = 7.0;
    private static final double BASE_ARMOR = 6.0;

    private static final double[] PHASE_MOVEMENT_SPEED = {BASE_MOVEMENT_SPEED, 0.40, 0.47};
    private static final double[] PHASE_ATTACK_DAMAGE = {BASE_ATTACK_DAMAGE, 10.0, 14.0};

    // радиус обнаружения цели (было 40)
    private static final double DETECTION_RANGE = 25.0;

    // === Рывок ("dash strike") — с фазы 2 ===
    private static final int[] DASH_COOLDOWN_TICKS = {0, 16 * 20, 9 * 20}; // фаза1 не использует рывок
    private static final double DASH_MIN_RANGE = 4.0;
    private static final double DASH_MAX_RANGE = 12.0;
    private static final int DASH_TRAVEL_TICKS = 14;
    private static final double DASH_HIT_RADIUS = 2.0;
    private static final double DASH_KNOCKBACK = 0.9;
    private static final float DASH_DAMAGE_BONUS = 4.0f;

    // === Атака "молниевые круги" (как шипы у Thorncaller, но вместо шипов —
    // удары молний; от исходной атаки оставлены только кольца-телеграфы у
    // земли). Радиус самой атаки и интервалы между кольцами увеличены
    // относительно оригинальной атаки шипов Thorncaller. ===
    private static final int[] LIGHTNING_COOLDOWN_TICKS = {18 * 20, 14 * 20, 10 * 20};
    private static final double LIGHTNING_TRIGGER_RANGE = 22.0;   // увеличенный радиус атаки (было 14 у шипов)

    private static final int LIGHTNING_RING_COUNT = 3;            // столько же колец, сколько было у шипов
    private static final int LIGHTNING_PER_RING = 8;
    private static final double LIGHTNING_RING_MIN_RADIUS = 3.0;      // было 2.0 у шипов
    private static final double LIGHTNING_RING_WIDTH = 1.8;
    private static final double LIGHTNING_RING_SPACING = 4.5;         // увеличенный интервал между кольцами (было 2.6)
    // итого кольца получаются примерно: 3.0-4.8, 7.5-9.3, 12.0-13.8 блоков от Monsterik

    private static final double LIGHTNING_TELEGRAPH_RADIUS = 0.6;     // радиус самого кольца-телеграфа у земли
    private static final int LIGHTNING_RISE_TICKS = 18;               // сколько тиков кольцо "предупреждает" перед ударом молнии
    private static final double LIGHTNING_HIT_RADIUS = 1.4;
    private static final float LIGHTNING_DAMAGE = 12.0f;

    private static final int GLOBAL_ATTACK_DELAY_TICKS = 2 * 20; // пауза между спец-способностями

    private static final Vector3f ENRAGE_PARTICLE_COLOR = new Vector3f(0.85f, 0.1f, 0.1f);
    private static final Vector3f LIGHTNING_PARTICLE_COLOR = new Vector3f(0.2f, 0.85f, 0.78f); // бирюзовый

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // полоса здоровья вверху экрана, видна всем игрокам, которые видят Monsterik
    private final ServerBossBar bossBar = new ServerBossBar(
        this.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.NOTCHED_10
    );

    private int phase = 1; // 1, 2 или 3

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
    private final List<double[]> lightningPositions = new ArrayList<>(); // [x, groundY, z]

    public MonsterikEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
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
        // босс — его не расталкивают
        return false;
    }

    @Override
    public boolean isFireImmune() {
        // не должен загораться от собственной атаки молниями
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // не получает урон от собственной атаки молниями (в т.ч. если разряд
        // ударил рядом с ним самим), обычный урон при этом проходит как обычно
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        // босс не деспавнится сам по себе (как ЕНДЕР-дракон/иссушитель) —
        // за его существование в единственном экземпляре отвечает
        // MonsterikBossManager
        return false;
    }

    // ==================== Полоса здоровья (boss bar) ====================

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

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            com.crimsonmoon.world.MonsterikBossManager.onMonsterikDied(serverWorld, this);
        }
    }

    // ==================== Тик: фазы + спец-способности ====================

    @Override
    public void tick() {
        super.tick();

        this.bossBar.setPercent(MathHelper.clamp(this.getHealth() / this.getMaxHealth(), 0.0f, 1.0f));

        if (this.getWorld().isClient) {
            return;
        }

        updatePhase();

        if (this.dashActive) {
            progressDash();
        }
        if (this.lightningAttackActive) {
            progressLightningRingAttack();
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

    /** Пересчитывает фазу по текущему проценту HP и применяет усиления/эффекты при смене. */
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
        boolean result = super.tryAttack(target);
        if (result) {
            this.triggerAnim("main_controller", "attack");
        }
        return result;
    }

    // ==================== Рывок ("dash strike") — с фазы 2 ====================

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
        // лёгкое ease-out, чтобы рывок не выглядел как телепорт
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

    // ==================== Атака "молниевые круги" ====================
    // Аналог шиповой атаки Thorncaller: несколько колец точек вокруг
    // Monsterik. От оригинала оставлены только кольца-телеграфы у земли
    // (без растущих шипов), сами шипы заменены ударами молний.

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

        // кольца-телеграфы у земли пульсируют бирюзовыми частицами всё время
        // "предупреждения", пока не ударят молнии
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

    /** Плоское бирюзовое кольцо частиц у земли — телеграф места будущего удара молнии. */
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

    // ==================== Звуки ====================

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

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::movementPredicate)
            .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.monsterik.attack")));
    }

    private PlayState movementPredicate(AnimationState<MonsterikEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        // отдельной "idle"-анимации нет — на месте и на ходу используем "walk",
        // а во время рывка и в фазе 3 на ходу — более резвый "run"
        if (this.dashActive) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.monsterik.run"));
        } else if (state.isMoving() && this.phase >= 2) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.monsterik.run"));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.monsterik.walk"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
