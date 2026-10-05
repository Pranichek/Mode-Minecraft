package com.crimsonmoon.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Взаимодействие Thorncaller с биомом джунглей (часть 1 — лазание по деревьям).
 *
 * Время от времени, когда цель находится не вплотную (иначе выгоднее просто
 * бить в ближнем бою), Thorncaller выбирает ближайший подходящий ствол дерева
 * и забирается по нему вверх, будто заходит на цель сверху / скрывается в
 * кроне. Это тактическое/атмосферное поведение — не отдельная атака.
 */
public class ThorncallerTreeClimbGoal extends Goal {

    private static final double SEARCH_RADIUS = 6.0;
    private static final int MIN_TRUNK_HEIGHT = 3;
    private static final int MAX_CLIMB_TICKS = 60;   // максимум 3 секунды подряд карабкаемся
    private static final int COOLDOWN_TICKS = 200;   // ~10с до следующей попытки
    private static final double CLIMB_SPEED = 0.16;
    private static final double MIN_TARGET_DIST_SQ = 16.0;  // не лезем на дерево, если цель совсем рядом
    private static final double MAX_TARGET_DIST_SQ = 576.0; // и не лезем, если цель слишком далеко

    private final ThorncallerEntity thorncaller;

    private BlockPos trunkBase;
    private int climbTicks;
    private int cooldown;

    public ThorncallerTreeClimbGoal(ThorncallerEntity thorncaller) {
        this.thorncaller = thorncaller;
        this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        LivingEntity target = thorncaller.getTarget();
        if (target == null || !target.isAlive() || !thorncaller.isOnGround()) {
            return false;
        }

        double distSq = thorncaller.squaredDistanceTo(target);
        if (distSq < MIN_TARGET_DIST_SQ || distSq > MAX_TARGET_DIST_SQ) {
            return false;
        }

        // редкое, "неожиданное" поведение — не срабатывает каждый раз, когда условия совпали
        if (thorncaller.getRandom().nextInt(140) != 0) {
            return false;
        }

        trunkBase = findClimbableTrunk();
        return trunkBase != null;
    }

    @Override
    public void start() {
        climbTicks = 0;
        if (trunkBase != null) {
            thorncaller.getNavigation().startMovingTo(
                trunkBase.getX() + 0.5, trunkBase.getY(), trunkBase.getZ() + 0.5, 1.1);
        }
    }

    @Override
    public boolean shouldContinue() {
        if (trunkBase == null || climbTicks >= MAX_CLIMB_TICKS) {
            return false;
        }
        double dx = (trunkBase.getX() + 0.5) - thorncaller.getX();
        double dz = (trunkBase.getZ() + 0.5) - thorncaller.getZ();
        return (dx * dx + dz * dz) < 25.0;
    }

    @Override
    public void tick() {
        if (trunkBase == null) {
            return;
        }

        double dx = (trunkBase.getX() + 0.5) - thorncaller.getX();
        double dz = (trunkBase.getZ() + 0.5) - thorncaller.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // сначала подходим вплотную к стволу
        if (horizDist > 1.6) {
            thorncaller.getNavigation().startMovingTo(
                trunkBase.getX() + 0.5, thorncaller.getY(), trunkBase.getZ() + 0.5, 1.1);
            return;
        }

        // достигли максимальной высоты ствола — дальше карабкаться некуда
        int climbedAbove = MathFloor(thorncaller.getY()) - trunkBase.getY();
        if (climbedAbove >= trunkHeight(thorncaller.getWorld(), trunkBase)) {
            climbTicks = MAX_CLIMB_TICKS;
            return;
        }

        climbTicks++;

        // прижимаемся к стволу и лезем вверх, будто цепляясь за кору
        double pushX = horizDist > 0.05 ? (dx / horizDist) * 0.06 : 0.0;
        double pushZ = horizDist > 0.05 ? (dz / horizDist) * 0.06 : 0.0;

        thorncaller.setVelocity(pushX, CLIMB_SPEED, pushZ);
        thorncaller.velocityModified = true;
        thorncaller.fallDistance = 0.0f;
    }

    @Override
    public void stop() {
        trunkBase = null;
        cooldown = COOLDOWN_TICKS;
        thorncaller.getNavigation().stop();
    }

    private static int MathFloor(double value) {
        int floor = (int) value;
        return value < floor ? floor - 1 : floor;
    }

    private BlockPos findClimbableTrunk() {
        BlockPos origin = thorncaller.getBlockPos();
        World world = thorncaller.getWorld();

        BlockPos best = null;
        long bestDistSq = Long.MAX_VALUE;

        int r = (int) SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos base = origin.add(dx, 0, dz);
                if (!isLog(world.getBlockState(base))) {
                    continue;
                }
                if (trunkHeight(world, base) < MIN_TRUNK_HEIGHT) {
                    continue;
                }
                long distSq = (long) dx * dx + (long) dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = base;
                }
            }
        }
        return best;
    }

    private static int trunkHeight(World world, BlockPos base) {
        int height = 0;
        BlockPos.Mutable mutable = base.mutableCopy();
        while (height < 12 && isLog(world.getBlockState(mutable))) {
            height++;
            mutable.move(net.minecraft.util.math.Direction.UP);
        }
        return height;
    }

    private static boolean isLog(BlockState state) {
        return state.isIn(BlockTags.LOGS);
    }
}
