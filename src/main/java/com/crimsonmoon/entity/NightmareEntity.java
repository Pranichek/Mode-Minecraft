package com.crimsonmoon.entity;

import com.crimsonmoon.ModSounds;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Nightmare — босс-сущность.
 * Постоянно преследует проклятого игрока, внезапно появляется за спиной,
 * не исчезает после смерти игрока, не задыхается и выкапывается ТОЛЬКО
 * если реально замурован в блоках (а не при каждом касании стены).
 */
public class NightmareEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean wasChasingTarget = false;

    private int appearCooldown = 200; // таймер до следующего появления за спиной
    private int digCooldown = 0;      // таймер проверки «замурован в блоках»

    private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.RED,
            BossBar.Style.PROGRESS
    ).setDarkenSky(true);

    public NightmareEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 50;
        this.setPersistent(); // босс не исчезает после смерти/ухода игрока
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // никогда не деспавнится сам по себе
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        // Босс не задыхается/не гибнет от застревания в блоках
        if (damageSource.isOf(DamageTypes.IN_WALL)) {
            return true;
        }
        return super.isInvulnerableTo(damageSource);
    }

    public static DefaultAttributeContainer.Builder createNightmareAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
            .add(EntityAttributes.GENERIC_ARMOR, 5.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 100.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new NightmareMeleeAttackGoal(this, 1.15, true));
        // Активно идёт к игроку даже без прямой видимости (вместо блуждания)
        this.goalSelector.add(4, new StalkPlayerGoal(this, 1.1));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);

        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            com.crimsonmoon.world.CrimsonMoonWorldState state = com.crimsonmoon.world.CrimsonMoonWorldState.getServerState(serverWorld.getServer());
            state.nightmareDefeated = true;
            state.markDirty();

            this.dropItem(com.crimsonmoon.ModItems.BLOOD_MACE);

            com.crimsonmoon.world.CrimsonMoonWorldState.checkAndDropKey(serverWorld, this);
        }
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
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        ServerWorld world = (ServerWorld) this.getWorld();

        // Выкапываемся, только если реально замурованы в блоках
        if (--this.digCooldown <= 0) {
            this.digCooldown = 2; // проверяем почти каждый тик
            freeSpaceAround(world);
        }

        boolean isChasingNow = this.getTarget() != null;
        if (isChasingNow && !this.wasChasingTarget) {
            world.playSound(null, this.getBlockPos(),
                ModSounds.NIGHTMARE_CHASE, SoundCategory.HOSTILE, 1.0f, this.getSoundPitch());
        }
        this.wasChasingTarget = isChasingNow;

        ServerPlayerEntity nearest = findNearestCursedPlayer(world);
        if (nearest == null) return;

        // Внезапное появление за спиной — только когда он игрока не преследует в упор
        if (this.getTarget() == null && --this.appearCooldown <= 0) {
            this.appearCooldown = 240 + world.random.nextInt(240); // 12–24 сек, непредсказуемо
            double dist = Math.sqrt(this.squaredDistanceTo(nearest));
            if (dist > 20 || world.random.nextInt(2) == 0) {
                boolean withDarkness = world.random.nextInt(4) == 0; // затемнение редкое, 1 из 4
                tryAppearBehind(world, nearest, withDarkness);
            }
        }
    }

    private ServerPlayerEntity findNearestCursedPlayer(ServerWorld world) {
        ServerPlayerEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (p.getWorld() != world) continue;
            if (!p.getCommandTags().contains("cursed_player")) continue;
            double d = this.squaredDistanceTo(p);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    /** Телепорт за спину игроку (5–9 блоков) с резким звуком. Изредка — короткое затемнение. */
    private void tryAppearBehind(ServerWorld world, ServerPlayerEntity player, boolean withDarkness) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        double lookX = -Math.sin(yawRad); // куда смотрит игрок
        double lookZ = Math.cos(yawRad);
        double perpX = -lookZ, perpZ = lookX; // вбок
        BlockPos ppos = player.getBlockPos();

        for (int i = 0; i < 24; i++) {
            double back = 5 + world.random.nextInt(5);   // 5–9 блоков за спиной
            double side = world.random.nextInt(5) - 2;   // немного вбок
            int bx = (int) Math.round(ppos.getX() - lookX * back + perpX * side);
            int bz = (int) Math.round(ppos.getZ() - lookZ * back + perpZ * side);
            int by = ppos.getY() + (world.random.nextInt(5) - 2);

            BlockPos pos = new BlockPos(bx, by, bz);
            BlockPos floor = pos.down();
            if (!world.getBlockState(floor).isSolidBlock(world, floor)) continue;
            if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) continue;

            this.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), 0.0f);
            world.playSound(null, pos, ModSounds.NIGHTMARE_CHASE, SoundCategory.HOSTILE, 1.0f, 0.7f);
            world.spawnParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.2, 0.4, 0.2, 0.01);

            if (withDarkness) {
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, 40, 0, false, false, true)); // всего 2 сек
            }
            return;
        }
    }

    /** Расчищает место, ТОЛЬКО если босс реально замурован (блоки в клетке ног и/или головы). */
    private void freeSpaceAround(ServerWorld world) {
        BlockPos feet = this.getBlockPos();

        // Замурован по-настоящему только если блоки прямо в клетке ног ИЛИ головы
        BlockState atFeet = world.getBlockState(feet);
        BlockState atHead = world.getBlockState(feet.up());
        boolean buriedFeet = !atFeet.isAir() && atFeet.getFluidState().isEmpty();
        boolean buriedHead = !atHead.isAir() && atHead.getFluidState().isEmpty();
        if (!buriedFeet && !buriedHead) return; // хотя бы одна клетка свободна — не копаем

        // Освобождаем коробку 3×3 в ширину и 3 блока в высоту
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos p = feet.add(x, y, z);
                    if (canBreak(world, p)) {
                        world.breakBlock(p, false); // false = без выпадения предметов
                    }
                }
            }
        }
    }

    /** Можно ли ломать этот блок (не трогаем воздух, жидкости и особо прочные блоки). */
    private boolean canBreak(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false; // не трогаем воду/лаву
        if (state.getHardness(world, pos) < 0) return false; // бедрок и т.п. (hardness -1)
        if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)
                || state.isOf(Blocks.REINFORCED_DEEPSLATE)) return false;
        return true;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (result) {
            this.triggerAnim("main_controller", "attack");
        }
        return result;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.NIGHTMARE_GROWL;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NIGHTMARE_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.NIGHTMARE_HURT;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::movementPredicate)
            .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.nightmare.attack")));
    }

    private PlayState movementPredicate(AnimationState<NightmareEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }
        if (state.isMoving()) {
            boolean isRunning = this.getTarget() != null;
            if (isRunning) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.nightmare.run"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.nightmare.walk"));
            }
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.nightmare.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}