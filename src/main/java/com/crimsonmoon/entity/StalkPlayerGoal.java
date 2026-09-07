package com.crimsonmoon.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * Заставляет Nightmare идти к ближайшему проклятому игроку по навигации —
 * даже если игрока не видно за стеной (в пещерах). Работает, только пока
 * у босса НЕТ боевой цели: как только он увидел игрока напрямую, управление
 * забирает обычный ActiveTargetGoal + атака.
 */
public class StalkPlayerGoal extends Goal {

    private final NightmareEntity mob;
    private final double speed;
    private ServerPlayerEntity target;
    private int recalcCooldown;

    public StalkPlayerGoal(NightmareEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) return false;
        this.target = findNearestCursedPlayer();
        return this.target != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null
            && this.target.isAlive()
            && mob.getTarget() == null
            && this.target.getWorld() == mob.getWorld()
            && mob.squaredDistanceTo(this.target) < 120.0 * 120.0;
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
            this.recalcCooldown = 10; // пересчитываем путь каждые полсекунды
            this.mob.getNavigation().startMovingTo(this.target, this.speed);
        }
    }

    private ServerPlayerEntity findNearestCursedPlayer() {
        if (!(mob.getWorld() instanceof ServerWorld world)) return null;
        ServerPlayerEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (p.getWorld() != world) continue;
            if (!p.getCommandTags().contains("cursed_player")) continue;
            double d = mob.squaredDistanceTo(p);
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }
}