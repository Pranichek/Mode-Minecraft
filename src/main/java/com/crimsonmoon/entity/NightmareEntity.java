package com.crimsonmoon.entity;

import com.crimsonmoon.ModSounds;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
// import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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
 * Nightmare — враждебная сущность высотой 3 блока.
 * Атакует и преследует игрока. Модель/анимации грузятся через GeckoLib
 * из assets/crimsonmoon/geo/nightmare.geo.json и
 * assets/crimsonmoon/animations/nightmare.animation.json — их можно
 * заменять файлами из Blockbench, ничего в коде менять не нужно.
 *
 * Названия анимаций, которые ищет контроллер ниже:
 *   animation.nightmare.idle
 *   animation.nightmare.walk
 *   animation.nightmare.attack
 * Если в своём Blockbench-проекте назовёшь анимации иначе — поменяй
 * строки в registerControllers() и в triggerAnim(...) ниже.
 */
public class NightmareEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private boolean wasChasingTarget = false;

    public NightmareEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createNightmareAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
            .add(EntityAttributes.GENERIC_ARMOR, 2.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new NightmareMeleeAttackGoal(this, 1.15, true));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            boolean isChasingNow = this.getTarget() != null;
            if (isChasingNow && !this.wasChasingTarget) {
                this.getWorld().playSound(
                    null, this.getBlockPos(),
                    ModSounds.NIGHTMARE_CHASE, SoundCategory.HOSTILE,
                    1.0f, this.getSoundPitch()
                );
            }
            this.wasChasingTarget = isChasingNow;
        }

        if (!this.getWorld().isClient) {
            boolean isChasingNow = this.getTarget() != null;
            if (isChasingNow && !this.wasChasingTarget) {
                // цель только что появилась — проигрываем звук погони один раз
                this.getWorld().playSound(
                    null, this.getBlockPos(),
                    ModSounds.NIGHTMARE_CHASE, SoundCategory.HOSTILE,
                    1.0f, this.getSoundPitch()
                );
            }
            this.wasChasingTarget = isChasingNow;
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

    // ---- звуки ----
    // Настоящие .ogg-файлы нужно положить сюда (см. README-NIGHTMARE.md):
    //   assets/crimsonmoon/sounds/nightmare/growl.ogg
    //   assets/crimsonmoon/sounds/nightmare/hurt.ogg
    //   assets/crimsonmoon/sounds/nightmare/chase.ogg
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

    // ---- GeckoLib ----
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
