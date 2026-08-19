package com.crimsonmoon.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;

public class BloodwalkerEntity extends ZombieEntity {

    public BloodwalkerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    // Характеристики моба — как у зомби, но чуть быстрее и злее
    public static DefaultAttributeContainer.Builder createBloodwalkerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
            .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    // Поведение: атака в ближнем бою + обычное блуждание
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, net.minecraft.entity.player.PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, net.minecraft.entity.player.PlayerEntity.class, true));
    }

    // Убираем зомби-звуки (эмбиент-стон, звук урона, звук смерти), шаги оставляем как есть
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
}