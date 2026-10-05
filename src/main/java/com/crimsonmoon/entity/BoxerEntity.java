package com.crimsonmoon.entity;

import com.crimsonmoon.world.CrimsonMoonWorldState;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BoxerEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int currentPhase = 1;
    private int attackAnimTicks = 0;
    private int specialCooldown = 40;
    private int chargeCooldown = 120;
    private int waveCooldown = 200;
    private int jumpCooldown = 160;
    private int digCooldown = 0;
    private int frustrationTicks = 0;

    private boolean isLeaping = false;

    private final ServerBossBar bossBar = (ServerBossBar) new ServerBossBar(
            this.getDisplayName(),
            BossBar.Color.RED,
            BossBar.Style.PROGRESS
    );

    public BoxerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 75;
        this.setPersistent();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(damageSource);
    }

    public static DefaultAttributeContainer.Builder createBoxerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 400.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 16.0)
            .add(EntityAttributes.GENERIC_ARMOR, 8.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
            .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 80.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.add(4, new BoxerStalkGoal(this, 1.15));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 20.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
        updatePhase();

        if (this.attackAnimTicks > 0) this.attackAnimTicks--;
        if (this.specialCooldown > 0) this.specialCooldown--;
        if (this.chargeCooldown > 0) this.chargeCooldown--;
        if (this.waveCooldown > 0) this.waveCooldown--;
        if (this.jumpCooldown > 0) this.jumpCooldown--;

        if (this.getWorld() instanceof ServerWorld sw) {
            if (--this.digCooldown <= 0) {
                this.digCooldown = 4;
                freeSpaceIfBuried(sw);
            }
            if (this.isLeaping && this.isOnGround()) {
                this.isLeaping = false;
                doShockwave(2.0f);
            }
        }

        LivingEntity target = this.getTarget();
        if (target == null || this.getWorld().isClient) return;

        double dist = this.distanceTo(target);

        boolean cantReach = dist < 18 && !this.canSee(target) && !this.isLeaping;
        if (cantReach) {
            this.frustrationTicks++;
        } else {
            this.frustrationTicks = Math.max(0, this.frustrationTicks - 2);
        }
        if (this.frustrationTicks >= 100) {
            this.frustrationTicks = 0;
            doRageTeleport(target);
            return;
        }

        if (this.jumpCooldown <= 0 && dist > 6 && dist < 22 && this.isOnGround()
                && this.getRandom().nextInt(3) == 0) {
            doLeap(target);
        }

        if (this.chargeCooldown <= 0 && dist > 4 && dist < 16 && !this.isLeaping
                && this.getRandom().nextInt(3) == 0) {
            doCharge(target);
        }

        if (this.currentPhase >= 2 && this.waveCooldown <= 0 && dist < 5 && !this.isLeaping) {
            doShockwave(1.0f);
        }
    }

    private void updatePhase() {
        float pct = this.getHealth() / this.getMaxHealth();
        int newPhase = pct > 0.66f ? 1 : (pct > 0.33f ? 2 : 3);
        if (newPhase != this.currentPhase) {
            this.currentPhase = newPhase;
            EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.32 + 0.07 * (newPhase - 1));
            }
            this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5f, 0.8f);
        }
    }

    private void doLeap(LivingEntity target) {
        this.jumpCooldown = 140 - this.currentPhase * 20;
        Vec3d dir = target.getPos().subtract(this.getPos());
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (horiz < 0.01) return;
        double speed = Math.min(horiz * 0.28, 2.2);
        Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
        this.setVelocity(flat.x * speed, 0.9, flat.z * speed);
        this.velocityModified = true;
        this.isLeaping = true;
        this.triggerAnim("main_controller", "aperkotik");
        this.attackAnimTicks = 16;
        this.getWorld().playSound(null, this.getBlockPos(),
            SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.2f, 1.0f);
    }

    private void doCharge(LivingEntity target) {
        this.chargeCooldown = 100 - this.currentPhase * 15;
        Vec3d dir = target.getPos().subtract(this.getPos()).normalize();
        this.setVelocity(dir.x * 1.6, 0.35, dir.z * 1.6);
        this.velocityModified = true;
        this.triggerAnim("main_controller", "comba");
        this.attackAnimTicks = 10;
        this.getWorld().playSound(null, this.getBlockPos(),
            SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.HOSTILE, 1.5f, 0.7f);
    }

    private void doShockwave(float mult) {
        this.waveCooldown = 160;
        this.triggerAnim("main_controller", "aperkotik");
        this.attackAnimTicks = 16;

        double radius = 4.0 * mult;

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.2, this.getZ(),
                (int) (6 * mult), radius * 0.5, 0.2, radius * 0.5, 0.0);
        }
        this.getWorld().playSound(null, this.getBlockPos(),
            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.3f, 1.0f);

        Box area = this.getBoundingBox().expand(radius, 2.0, radius);
        float dmg = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 0.75f * mult;
        for (PlayerEntity p : this.getWorld().getEntitiesByClass(PlayerEntity.class, area, e -> e.isAlive())) {
            p.damage(this.getDamageSources().mobAttack(this), dmg);
            Vec3d push = p.getPos().subtract(this.getPos()).normalize();
            p.setVelocity(push.x * 0.9 * mult, 0.7, push.z * 0.9 * mult);
            p.velocityModified = true;
        }
    }

    private void doRageTeleport(LivingEntity target) {
        Vec3d behind = target.getPos().subtract(target.getRotationVec(1.0f).multiply(1.2));
        this.refreshPositionAndAngles(behind.x, target.getY(), behind.z, this.getYaw(), 0);

        this.getWorld().playSound(null, this.getBlockPos(),
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.6f);
        this.getWorld().playSound(null, this.getBlockPos(),
            SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5f, 0.7f);

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
        }

        this.tryAttack(target);
        doShockwave(1.2f);
        this.triggerAnim("main_controller", "grab-Crab");
        this.attackAnimTicks = 16;
    }

    private void freeSpaceIfBuried(ServerWorld world) {
        BlockPos feet = this.getBlockPos();
        BlockState atFeet = world.getBlockState(feet);
        BlockState atHead = world.getBlockState(feet.up());
        boolean buriedFeet = !atFeet.isAir() && atFeet.getFluidState().isEmpty();
        boolean buriedHead = !atHead.isAir() && atHead.getFluidState().isEmpty();
        if (!buriedFeet && !buriedHead) return;

        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos p = feet.add(x, y, z);
                    BlockState st = world.getBlockState(p);
                    if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                    if (st.getHardness(world, p) < 0) continue; 
                    if (st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.REINFORCED_DEEPSLATE)) continue;
                    world.breakBlock(p, false);
                }
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (!result || this.getWorld().isClient) return result;

        String anim = "comba";

        if (this.specialCooldown <= 0) {
            int roll = this.getRandom().nextInt(4);
            if (this.currentPhase >= 3 && roll < 2) {
                anim = "grab-Crab";
                if (target instanceof LivingEntity living) {
                    Vec3d pull = this.getPos().subtract(living.getPos()).normalize().multiply(1.6);
                    living.setVelocity(pull.x, 0.4, pull.z);
                    living.velocityModified = true;
                    living.damage(this.getDamageSources().mobAttack(this),
                        (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 0.5f);
                }
                this.specialCooldown = 50;
            } else if (this.currentPhase >= 2 && roll < 2) {
                anim = "aperkotik";
                if (target instanceof LivingEntity living) {
                    living.setVelocity(living.getVelocity().x, 1.1, living.getVelocity().z);
                    living.velocityModified = true;
                }
                this.specialCooldown = 45;
            }
        }

        this.triggerAnim("main_controller", anim);
        this.attackAnimTicks = 12;
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(serverWorld.getServer());
            state.boxerDefeated = true;
            state.markDirty();

            this.dropItem(com.crimsonmoon.ModItems.BLOOD_BOW);

            CrimsonMoonWorldState.checkAndDropKey(serverWorld, this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::movementPredicate)
            .triggerableAnim("comba", RawAnimation.begin().thenPlay("comba"))
            .triggerableAnim("aperkotik", RawAnimation.begin().thenPlay("aperkotik"))
            .triggerableAnim("grab-Crab", RawAnimation.begin().thenPlay("grab-Crab")));
    }

    private PlayState movementPredicate(AnimationState<BoxerEntity> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }
        if (state.isMoving()) {
            boolean running = this.getTarget() != null;
            state.getController().setAnimation(
                RawAnimation.begin().thenLoop(running ? "run" : "walk"));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}