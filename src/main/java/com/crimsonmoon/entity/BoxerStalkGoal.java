package com.crimsonmoon.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * Ведёт боксёра к ближайшему игроку по навигации, даже без прямой видимости
 * (чтобы не застревал и не терял цель за стенами). Работает, только когда у
 * босса нет боевой цели в упор — иначе управление забирает обычная атака.
 */
public class BoxerStalkGoal extends Goal {

    private final BoxerEntity mob;
    private final double speed;
    private ServerPlayerEntity target;
    private int recalcCooldown;

    public BoxerStalkGoal(BoxerEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) return false;
        this.target = findNearestPlayer();
        return this.target != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null
            && this.target.isAlive()
            && mob.getTarget() == null
            && this.target.getWorld() == mob.getWorld()
            && mob.squaredDistanceTo(this.target) < 100.0 * 100.0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;
        this.mob.getLookControl().lookAt(this.target, 30.0f, 30.0f);
        if (--this.recalcCooldown <= 0) {
            this.recalcCooldown = 10;
            this.mob.getNavigation().startMovingTo(this.target, this.speed);
        }
    }

    private ServerPlayerEntity findNearestPlayer() {
        if (!(mob.getWorld() instanceof ServerWorld world)) return null;
        ServerPlayerEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (p.getWorld() != world) continue;
            if (p.isSpectator() || p.isCreative()) continue; // не гоняемся за наблюдателем/креативом
            double d = mob.squaredDistanceTo(p);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }
}