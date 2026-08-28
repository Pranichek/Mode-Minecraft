package com.crimsonmoon.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Взаимодействие Thorncaller с биомом джунглей (часть 2 — укрытие за деревьями).
 *
 * Раз в некоторое время, пока цель на среднем расстоянии и видит Thorncaller,
 * он перебегает к ближайшему стволу/листве так, чтобы оказаться на
 * противоположной от цели стороне — "прячется" и на пару секунд пропадает
 * из поля зрения, имитируя охоту из засады, после чего возвращается
 * к обычному поведению (в том числе к атакам).
 */
public class ThorncallerTreeHideGoal extends Goal {

    private static final double SEARCH_RADIUS = 8.0;
    private static final double MIN_TARGET_DIST_SQ = 25.0;   // 5 блоков
    private static final double MAX_TARGET_DIST_SQ = 400.0;  // 20 блоков
    private static final int HIDE_TICKS = 50;                // ~2.5с прячемся
    private static final int COOLDOWN_TICKS = 160;           // ~8с до следующей попытки

    private final ThorncallerEntity thorncaller;

    private BlockPos hideSpot;
    private int hideTicks;
    private int cooldown;

    public ThorncallerTreeHideGoal(ThorncallerEntity thorncaller) {
        this.thorncaller = thorncaller;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        LivingEntity target = thorncaller.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        double distSq = thorncaller.squaredDistanceTo(target);
        if (distSq < MIN_TARGET_DIST_SQ || distSq > MAX_TARGET_DIST_SQ) {
            return false;
        }

        if (!thorncaller.getVisibilityCache().canSee(target)) {
            return false; // прятаться от того, кто и так тебя не видит, незачем
        }

        if (thorncaller.getRandom().nextInt(160) != 0) {
            return false;
        }

        hideSpot = findHideSpot(target);
        return hideSpot != null;
    }

    @Override
    public void start() {
        hideTicks = 0;
        if (hideSpot != null) {
            thorncaller.getNavigation().startMovingTo(
                hideSpot.getX() + 0.5, hideSpot.getY(), hideSpot.getZ() + 0.5, 1.15);
        }
    }

    @Override
    public boolean shouldContinue() {
        if (hideSpot == null || hideTicks >= HIDE_TICKS) {
            return false;
        }
        // если укрытие уже не даёт преимущества (цель тоже подошла вплотную) — прекращаем прятаться
        LivingEntity target = thorncaller.getTarget();
        return target == null || thorncaller.squaredDistanceTo(target) > 9.0;
    }

    @Override
    public void tick() {
        hideTicks++;

        if (thorncaller.getNavigation().isIdle() && hideSpot != null) {
            double dx = (hideSpot.getX() + 0.5) - thorncaller.getX();
            double dz = (hideSpot.getZ() + 0.5) - thorncaller.getZ();
            if (dx * dx + dz * dz > 4.0) {
                thorncaller.getNavigation().startMovingTo(
                    hideSpot.getX() + 0.5, hideSpot.getY(), hideSpot.getZ() + 0.5, 1.15);
            }
        }

        LivingEntity target = thorncaller.getTarget();
        if (target != null) {
            // выглядывает из-за укрытия, но не срывается в погоню раньше времени
            thorncaller.getLookControl().lookAt(target, 30.0f, 30.0f);
        }
    }

    @Override
    public void stop() {
        hideSpot = null;
        cooldown = COOLDOWN_TICKS;
    }

    /**
     * Ищем позицию рядом с ближайшим подходящим "укрытием" (ствол дерева или
     * густая листва) такую, чтобы блок укрытия оказался между Thorncaller
     * и целью.
     */
    private BlockPos findHideSpot(LivingEntity target) {
        BlockPos origin = thorncaller.getBlockPos();
        World world = thorncaller.getWorld();

        BlockPos bestCover = null;
        long bestDistSq = Long.MAX_VALUE;

        int r = (int) SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos cover = origin.add(dx, 0, dz);
                if (!isCoverBlock(world.getBlockState(cover))) {
                    continue;
                }
                long distSq = (long) dx * dx + (long) dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestCover = cover;
                }
            }
        }

        if (bestCover == null) {
            return null;
        }

        // точка по другую сторону укрытия относительно цели
        double awayX = (bestCover.getX() + 0.5) - target.getX();
        double awayZ = (bestCover.getZ() + 0.5) - target.getZ();
        double len = Math.sqrt(awayX * awayX + awayZ * awayZ);
        if (len < 0.001) {
            return null;
        }
        awayX /= len;
        awayZ /= len;

        BlockPos spot = new BlockPos(
            (int) Math.floor(bestCover.getX() + 0.5 + awayX),
            bestCover.getY(),
            (int) Math.floor(bestCover.getZ() + 0.5 + awayZ)
        );

        boolean solidFloor = !world.getBlockState(spot.down()).getCollisionShape(world, spot.down()).isEmpty();
        return solidFloor ? spot : null;
    }

    private static boolean isCoverBlock(BlockState state) {
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES);
    }
}
