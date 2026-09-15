package com.crimsonmoon.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Атака 1 (призыв, summonMinions): раз в SUMMON_COOLDOWN_TICKS призывает
 * миньонов, вокруг заспавненного миньона появляются зелёные частицы.
 *
 * Атака 2 (шипы из земли, startSpikeAttack): раз в SPIKE_ATTACK_COOLDOWN_TICKS
 * выпускает вокруг себя SPIKE_RING_COUNT колец по SPIKE_PER_RING шипов
 * из зелёных частиц (2-3 ряда шипов, ближний/средний/дальний), шипы
 * толстые у основания и сужаются к верхушке. Наносят урон игроку,
 * если тот стоит рядом с шипом в момент, когда шип почти вырос.
 *
 * Атака 3 ("игольчатый шторм", startStormAttack): раз в
 * STORM_ATTACK_COOLDOWN_TICKS пускает от себя расширяющееся во все
 * стороны (360°) кольцо снарядов. Кольцо "высокое" (частицы рисуются
 * на нескольких высотах от земли до ~1.7 блока), поэтому просто
 * прыжком его не перепрыгнуть. При попадании — урон + Slowness
 * (см. STORM_SLOWNESS_AMPLIFIER) на 5 секунд.
 *
 * Атака 4 ("шиповая пика", startLanceAttack): раз в
 * LANCE_ATTACK_COOLDOWN_TICKS выпускает по прямой линии в сторону цели
 * несколько быстрорастущих шипов (прицельный, а не круговой удар —
 * от него нужно уворачиваться в сторону). При попадании — урон +
 * Poison на несколько секунд.
 *
 * Модель/анимации грузятся через GeckoLib из:
 *   assets/crimsonmoon/geo/thorncaller.geo.json
 *   assets/crimsonmoon/animations/thorncaller.animation.json
 *   assets/crimsonmoon/textures/entity/thorncaller/thorncaller.png
 * (см. ThorncallerModel / ThorncallerEntityRenderer).
 *
 * Спавнится ТОЛЬКО в биомах джунглей — проверка сделана в
 * ThorncallerSpawnManager (BiomeTags.IS_JUNGLE).
 */
public class ThorncallerEntity extends ZombieEntity implements GeoEntity {

    // === Общая пауза между ЛЮБЫМИ двумя атаками ===
    // Не даёт кастовать способности одну за другой без перерыва —
    // после любой атаки должно пройти хотя бы это время, прежде чем
    // начнётся следующая (неважно, какая именно).
    private static final int GLOBAL_ATTACK_DELAY_TICKS = 4 * 20; // 4 секунды

    // === Настройка призыва подкрепления (атака 1) ===
    public static EntityType<? extends MobEntity> MINION_TYPE = EntityType.ZOMBIE;

    private static final String MINION_TAG = "thorncaller_minion";
    private static final int SUMMON_COOLDOWN_TICKS = 25 * 20; // раз в 25 секунд
    private static final int MINIONS_PER_CAST = 2;
    private static final int MAX_ALIVE_MINIONS = 6;   // сколько живых миньонов допускается рядом
    private static final double SUMMON_SEARCH_RADIUS = 3.0;
    private static final double MINION_TRACK_RADIUS = 24.0;

    // === Настройка атаки шипами вокруг себя (атака 2) ===
    private static final int SPIKE_ATTACK_COOLDOWN_TICKS = 20 * 20; // раз в 20 секунд
    private static final double SPIKE_TRIGGER_RANGE = 14.0;         // применяется только если цель не дальше этого

    // шипы стоят в несколько колец (рядов) вокруг Thorncaller,
    // а не в один ряд — визуально "поле шипов" получается гуще и шире
    private static final int SPIKE_RING_COUNT = 3;                  // сколько колец (рядов) шипов
    private static final int SPIKE_PER_RING = 8;                    // шипов в одном кольце
    private static final double SPIKE_RING_MIN_RADIUS = 2.0;        // расстояние до ближнего кольца
    private static final double SPIKE_RING_WIDTH = 1.6;             // разброс расстояния внутри одного кольца
    private static final double SPIKE_RING_SPACING = 2.6;           // на сколько дальше начинается следующее кольцо
    // итого кольца получаются примерно: 2.0-3.6, 4.6-6.2, 7.2-8.8 блоков от Thorncaller

    private static final double SPIKE_HEIGHT = 1.5;                 // высота шипа в блоках
    private static final double SPIKE_BASE_RADIUS = 0.55;           // толщина у основания
    private static final double SPIKE_TIP_RADIUS = 0.06;            // толщина на верхушке
    private static final int SPIKE_RISE_TICKS = 12;                 // сколько тиков шип растёт до полной высоты
    private static final int SPIKE_LINGER_TICKS = 10;               // сколько ещё держится на полной высоте
    private static final double SPIKE_HIT_RADIUS = 1.1;             // радиус, в котором шип наносит урон
    private static final float SPIKE_DAMAGE = 10.0f;

    // === Настройка "игольчатого шторма" — атака 3 ===
    private static final int STORM_ATTACK_COOLDOWN_TICKS = 28 * 20;  // раз в 28 секунд
    private static final double STORM_TRIGGER_RANGE = 14.0;
    private static final int STORM_PROJECTILE_COUNT = 24;            // "снарядов" по кругу (шаг 15°)
    private static final double STORM_SPEED_PER_TICK = 0.45;         // скорость расширения кольца, блоков/тик
    private static final double STORM_MAX_RADIUS = 12.0;
    private static final double STORM_HIT_BAND = 0.6;                // толщина полосы поражения кольца
    private static final float STORM_DAMAGE = 5.0f;
    private static final int STORM_SLOWNESS_AMPLIFIER = 3;           // 3 = Slowness IV (0 = I)
    private static final int STORM_SLOWNESS_DURATION_TICKS = 5 * 20; // 5 секунд
    // высоты, на которых рисуется "стена" кольца — от земли и выше обычного прыжка (~1.25 блока),
    // поэтому прыжком кольцо не перепрыгнуть
    private static final double[] STORM_WALL_HEIGHTS = {0.1, 0.5, 0.9, 1.3, 1.7};

    // === Настройка "шиповой пики" — прицельная атака 4 ===
    private static final int LANCE_ATTACK_COOLDOWN_TICKS = 22 * 20; // раз в 22 секунды
    private static final double LANCE_TRIGGER_RANGE = 9.0;
    private static final int LANCE_SPIKE_COUNT = 5;        // шипов в линии на цель
    private static final double LANCE_SPIKE_SPACING = 1.4; // расстояние между шипами в линии
    private static final double LANCE_START_DISTANCE = 1.5; // первый шип не прямо под ногами, а чуть впереди
    private static final double LANCE_SPIKE_HEIGHT = 1.6;
    private static final double LANCE_BASE_RADIUS = 0.5;
    private static final double LANCE_TIP_RADIUS = 0.06;
    private static final int LANCE_RISE_TICKS = 6;          // растут быстрее обычных шипов — это прицельный удар
    private static final int LANCE_LINGER_TICKS = 8;
    private static final double LANCE_HIT_RADIUS = 1.0;
    private static final float LANCE_DAMAGE = 5.0f;
    private static final int LANCE_POISON_AMPLIFIER = 1;          // 1 = Poison II
    private static final int LANCE_POISON_DURATION_TICKS = 4 * 20; // 4 секунды

    private static final Vector3f THORN_PARTICLE_COLOR = new Vector3f(0.16f, 0.85f, 0.25f);
    private static final Vector3f STORM_PARTICLE_COLOR = new Vector3f(0.55f, 0.85f, 0.15f);

    private int globalAttackDelay = 0; // общая пауза между любыми атаками

    private int summonCooldown = 60;
    private int spikeAttackCooldown = 100;
    private int stormAttackCooldown = 140;
    private int lanceAttackCooldown = 180;

    private boolean spikeAttackActive = false;
    private int spikeAttackProgress = 0;
    private final List<double[]> spikePositions = new ArrayList<>(); // [x, groundY, z]
    private boolean[] spikeDamageDealt = new boolean[0];

    private boolean stormAttackActive = false;
    private double stormRadius = 0.0;
    private final Set<UUID> stormHitPlayers = new HashSet<>();

    private boolean lanceAttackActive = false;
    private int lanceAttackProgress = 0;
    private final List<double[]> lancePositions = new ArrayList<>(); // [x, groundY, z]
    private boolean[] lanceDamageDealt = new boolean[0];

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

     // полоса здоровья вверху экрана (как у Иссушителя/Дракона),
    // видна всем игрокам, которые видят Thorncaller
    private final ServerBossBar bossBar = new ServerBossBar(
        this.getDisplayName(), BossBar.Color.GREEN, BossBar.Style.NOTCHED_10
    );


    public ThorncallerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createThorncallerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0)
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
        // Взаимодействие с биомом джунглей: карабканье по деревьям и укрытие за
        // стволами/листвой. Обе используют управление MOVE, поэтому конкурируют
        // за приоритет с ближним боем и блужданием, но не мешают атакам-способностям
        // (те не завязаны на goalSelector, см. tick()).
        this.goalSelector.add(3, new ThorncallerTreeClimbGoal(this));
        this.goalSelector.add(4, new ThorncallerTreeHideGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
            com.crimsonmoon.world.ThorncallerSpawnManager.onThorncallerDied(serverWorld, this);

            com.crimsonmoon.world.ThorncallerBossState thornState = com.crimsonmoon.world.ThorncallerBossState.get(serverWorld.getServer().getOverworld());
            thornState.setDefeated(true);

            this.dropItem(com.crimsonmoon.ModItems.BLOOD_STAFF);

            com.crimsonmoon.world.CrimsonMoonWorldState.checkAndDropKey(serverWorld, this);
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.bossBar.setPercent(MathHelper.clamp(this.getHealth() / this.getMaxHealth(), 0.0f, 1.0f));

        if (this.getWorld().isClient) {
            return;
        }

        // прогресс уже запущенных атак идёт каждый тик независимо от кулдаунов
        if (this.spikeAttackActive) {
            progressSpikeAttack();
        }
        if (this.stormAttackActive) {
            progressStormAttack();
        }
        if (this.lanceAttackActive) {
            progressLanceAttack();
        }

        if (this.globalAttackDelay > 0) {
            this.globalAttackDelay--;
        }
        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }
        if (this.spikeAttackCooldown > 0) {
            this.spikeAttackCooldown--;
        }
        if (this.stormAttackCooldown > 0) {
            this.stormAttackCooldown--;
        }
        if (this.lanceAttackCooldown > 0) {
            this.lanceAttackCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        // не начинаем новую атаку, пока не прошла общая пауза после предыдущей
        if (this.globalAttackDelay > 0) {
            return;
        }

        double distanceToTarget = this.distanceTo(target);

        if (this.summonCooldown <= 0) {
            summonMinions();
            this.summonCooldown = SUMMON_COOLDOWN_TICKS;
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
            return;
        }

        if (!this.spikeAttackActive && this.spikeAttackCooldown <= 0
            && distanceToTarget <= SPIKE_TRIGGER_RANGE) {
            startSpikeAttack();
            this.spikeAttackCooldown = SPIKE_ATTACK_COOLDOWN_TICKS;
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
            return;
        }

        if (!this.stormAttackActive && this.stormAttackCooldown <= 0
            && distanceToTarget <= STORM_TRIGGER_RANGE) {
            startStormAttack();
            this.stormAttackCooldown = STORM_ATTACK_COOLDOWN_TICKS;
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
            return;
        }

        if (!this.lanceAttackActive && this.lanceAttackCooldown <= 0
            && distanceToTarget <= LANCE_TRIGGER_RANGE) {
            startLanceAttack(target);
            this.lanceAttackCooldown = LANCE_ATTACK_COOLDOWN_TICKS;
            this.globalAttackDelay = GLOBAL_ATTACK_DELAY_TICKS;
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

    // ==================== Атака 2: шипы из земли (3 ряда вокруг себя) ====================

    private void startSpikeAttack() {
        spikePositions.clear();

        double baseAngle = this.random.nextDouble() * Math.PI * 2;
        double angleStep = (Math.PI * 2) / SPIKE_PER_RING;

        for (int ring = 0; ring < SPIKE_RING_COUNT; ring++) {
            // каждое следующее кольцо проворачиваем на половину шага, чтобы шипы
            // не стояли ровными "спицами" одна за другой, а перекрывали промежутки
            double ringAngleOffset = baseAngle + (angleStep / 2.0) * ring;
            double ringMinRadius = SPIKE_RING_MIN_RADIUS + ring * SPIKE_RING_SPACING;
            double ringMaxRadius = ringMinRadius + SPIKE_RING_WIDTH;

            for (int i = 0; i < SPIKE_PER_RING; i++) {
                double angle = ringAngleOffset + angleStep * i + (this.random.nextDouble() - 0.5) * angleStep * 0.3;
                double dist = ringMinRadius + this.random.nextDouble() * (ringMaxRadius - ringMinRadius);

                double x = this.getX() + Math.cos(angle) * dist;
                double z = this.getZ() + Math.sin(angle) * dist;
                double groundY = findSpikeGroundY(x, z);

                spikePositions.add(new double[]{x, groundY, z});
            }
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
            spawnSpikeParticles(world, spike[0], spike[1], spike[2], currentHeight, SPIKE_BASE_RADIUS, SPIKE_TIP_RADIUS);

            // урон наносится один раз, когда шип почти полностью вырос
            if (!spikeDamageDealt[i] && growFraction >= 0.75) {
                dealAreaDamage(world, spike[0], spike[1], spike[2], SPIKE_HIT_RADIUS, SPIKE_HEIGHT, SPIKE_DAMAGE, null, 0, 0);
                spikeDamageDealt[i] = true;
            }
        }

        if (spikeAttackProgress >= SPIKE_RISE_TICKS + SPIKE_LINGER_TICKS) {
            spikeAttackActive = false;
        }
    }

    // ==================== Атака 3: игольчатый шторм (360°, не перепрыгнуть) ====================

    private void startStormAttack() {
        stormAttackActive = true;
        stormRadius = 0.0;
        stormHitPlayers.clear();

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_EVOKER_CAST_SPELL,
            SoundCategory.HOSTILE,
            1.3f, 0.8f
        );
    }

    private void progressStormAttack() {
        ServerWorld world = (ServerWorld) this.getWorld();
        stormRadius += STORM_SPEED_PER_TICK;

        spawnStormRingParticles(world, stormRadius);

        // проверяем игроков, через которых кольцо прошло именно в этот тик
        double innerEdge = stormRadius - STORM_HIT_BAND;
        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isAlive() || stormHitPlayers.contains(player.getUuid())) {
                continue;
            }

            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            if (horizontalDist >= innerEdge && horizontalDist <= stormRadius
                && Math.abs(player.getY() - this.getY()) <= 2.5) {
                player.damage(this.getDamageSources().mobAttack(this), STORM_DAMAGE);
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, STORM_SLOWNESS_DURATION_TICKS, STORM_SLOWNESS_AMPLIFIER, false, true, true
                ));
                stormHitPlayers.add(player.getUuid());
            }
        }

        if (stormRadius >= STORM_MAX_RADIUS) {
            stormAttackActive = false;
        }
    }

    private void spawnStormRingParticles(ServerWorld world, double radius) {
        DustParticleEffect stormDust = new DustParticleEffect(STORM_PARTICLE_COLOR, 1.0f);
        double baseY = this.getY();

        for (int i = 0; i < STORM_PROJECTILE_COUNT; i++) {
            double angle = (Math.PI * 2 / STORM_PROJECTILE_COUNT) * i;
            double px = this.getX() + Math.cos(angle) * radius;
            double pz = this.getZ() + Math.sin(angle) * radius;

            for (double yOffset : STORM_WALL_HEIGHTS) {
                world.spawnParticles(stormDust, px, baseY + yOffset, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    // ==================== Атака 4: шиповая пика (прицельная линия) ====================

    private void startLanceAttack(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            return;
        }
        double dirX = dx / len;
        double dirZ = dz / len;

        lancePositions.clear();
        for (int i = 0; i < LANCE_SPIKE_COUNT; i++) {
            double dist = LANCE_START_DISTANCE + i * LANCE_SPIKE_SPACING;
            double x = this.getX() + dirX * dist;
            double z = this.getZ() + dirZ * dist;
            double groundY = findSpikeGroundY(x, z);

            lancePositions.add(new double[]{x, groundY, z});
        }

        lanceDamageDealt = new boolean[lancePositions.size()];
        lanceAttackActive = true;
        lanceAttackProgress = 0;

        this.triggerAnim("main_controller", "attack");

        this.getWorld().playSound(
            null, this.getBlockPos(),
            SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,
            SoundCategory.HOSTILE,
            1.2f, 1.1f
        );
    }

    private void progressLanceAttack() {
        lanceAttackProgress++;
        ServerWorld world = (ServerWorld) this.getWorld();

        double growFraction = Math.min(1.0, lanceAttackProgress / (double) LANCE_RISE_TICKS);
        double currentHeight = LANCE_SPIKE_HEIGHT * growFraction;

        for (int i = 0; i < lancePositions.size(); i++) {
            double[] spike = lancePositions.get(i);
            spawnSpikeParticles(world, spike[0], spike[1], spike[2], currentHeight, LANCE_BASE_RADIUS, LANCE_TIP_RADIUS);

            if (!lanceDamageDealt[i] && growFraction >= 0.7) {
                dealAreaDamage(
                    world, spike[0], spike[1], spike[2],
                    LANCE_HIT_RADIUS, LANCE_SPIKE_HEIGHT, LANCE_DAMAGE,
                    StatusEffects.POISON, LANCE_POISON_AMPLIFIER, LANCE_POISON_DURATION_TICKS
                );
                lanceDamageDealt[i] = true;
            }
        }

        if (lanceAttackProgress >= LANCE_RISE_TICKS + LANCE_LINGER_TICKS) {
            lanceAttackActive = false;
        }
    }

    // ==================== Общие помощники для "шипастых" атак (2 и 4) ====================

    /** Рисует один растущий шип кольцами частиц — толстый у земли, тонкий на верхушке. */
    private void spawnSpikeParticles(ServerWorld world, double x, double groundY, double z, double currentHeight, double baseRadius, double tipRadius) {
        if (currentHeight <= 0.0) {
            return;
        }

        DustParticleEffect thornDust = new DustParticleEffect(THORN_PARTICLE_COLOR, 1.3f);

        int rings = Math.max(2, (int) Math.ceil(currentHeight * 6));
        for (int r = 0; r <= rings; r++) {
            double t = r / (double) rings; // 0 у земли, 1 на верхушке шипа
            double y = groundY + currentHeight * t;
            double radius = MathHelper.lerp(t, baseRadius, tipRadius);

            int pointsInRing = radius > 0.2 ? 8 : (radius > 0.1 ? 6 : 3);
            for (int p = 0; p < pointsInRing; p++) {
                double angle = (Math.PI * 2 / pointsInRing) * p;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                world.spawnParticles(thornDust, px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * Наносит урон всем игрокам рядом с точкой (например, у основания шипа).
     * Если statusEffect не null — дополнительно накладывает его.
     */
    private void dealAreaDamage(ServerWorld world, double x, double groundY, double z, double hitRadius, double height, float damage, net.minecraft.entity.effect.StatusEffect statusEffect, int amplifier, int durationTicks) {
        Box hitBox = new Box(
            x - hitRadius, groundY - 0.5, z - hitRadius,
            x + hitRadius, groundY + height + 0.5, z + hitRadius
        );

        List<PlayerEntity> hitPlayers = world.getEntitiesByClass(PlayerEntity.class, hitBox, PlayerEntity::isAlive);
        for (PlayerEntity player : hitPlayers) {
            player.damage(this.getDamageSources().mobAttack(this), damage);
            player.addVelocity(0.0, 0.35, 0.0);
            player.velocityModified = true;

            if (statusEffect != null) {
                player.addStatusEffect(new StatusEffectInstance(statusEffect, durationTicks, amplifier, false, true, true));
            }
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