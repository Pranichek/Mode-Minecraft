package com.crimsonmoon.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.PathAwareEntity;


public class NightmareMeleeAttackGoal extends MeleeAttackGoal {

    private static final double ATTACK_RANGE_SQUARED = 5.0; // ~2.24 блока

    public NightmareMeleeAttackGoal(PathAwareEntity mob, double speed, boolean pauseWhenMobIdle) {
        super(mob, speed, pauseWhenMobIdle);
    }

    @Override
    protected double getSquaredMaxAttackDistance(LivingEntity entity) {
        return ATTACK_RANGE_SQUARED;
    }
}