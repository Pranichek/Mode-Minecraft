package com.crimsonmoon.world;

import com.crimsonmoon.ModEntities;
import com.crimsonmoon.entity.ThorncallerEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.registry.tag.BiomeTags;

public class ThorncallerSpawnManager {

    private static final int CHECK_INTERVAL_TICKS = 45 * 20; // раз в 45 секунд проверяем шанс
    private static final float SPAWN_CHANCE = 0.35f;

    private static final double MIN_DISTANCE = 14.0;
    private static final double MAX_DISTANCE = 20.0;
    private static final double OUT_OF_SIGHT_HALF_ANGLE = 100.0; // насколько широкий сектор "вне поля зрения" сзади/сбоку
    private static final int MAX_PLACEMENT_ATTEMPTS = 16;
    private static final int VERTICAL_SEARCH_RANGE = 5;

    private static final Map<UUID, Integer> timers = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ThorncallerSpawnManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!server.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            int ticks = timers.merge(id, 1, Integer::sum);

            if (ticks < CHECK_INTERVAL_TICKS) {
                continue;
            }
            timers.put(id, 0);

            if (player.getRandom().nextFloat() >= SPAWN_CHANCE) {
                continue;
            }

            trySpawn((ServerWorld) player.getWorld(), player);
        }
    }

    private static void trySpawn(ServerWorld world, ServerPlayerEntity player) {
    BlockPos pos = findOutOfSightPosition(world, player);
    if (pos == null) {
        return;
    }

    // Thorncaller — обитатель джунглей, спавним ТОЛЬКО в биомах джунглей
    // (jungle / sparse_jungle / bamboo_jungle попадают в этот тег).
    if (!world.getBiome(pos).isIn(BiomeTags.IS_JUNGLE)) {
        return;
    }

    ThorncallerEntity thorncaller = ModEntities.THORNCALLER.create(world);
    if (thorncaller == null) {
        return;
    }

    thorncaller.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
    world.spawnEntity(thorncaller);
}

    // Ищем точку, которая находится ВНЕ поля зрения игрока —
    // то есть за пределами конуса перед ним (тот же принцип, что и у Watcher).
    private static BlockPos findOutOfSightPosition(ServerWorld world, ServerPlayerEntity player) {
        float playerYawRad = (float) Math.toRadians(MathHelper.wrapDegrees(player.getYaw()));

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            double angleOffsetDeg = OUT_OF_SIGHT_HALF_ANGLE
                + player.getRandom().nextDouble() * (360.0 - 2 * OUT_OF_SIGHT_HALF_ANGLE);
            double angleRad = playerYawRad + Math.toRadians(angleOffsetDeg);

            double distance = MIN_DISTANCE + player.getRandom().nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);

            double dx = -Math.sin(angleRad) * distance;
            double dz = Math.cos(angleRad) * distance;

            int x = MathHelper.floor(player.getX() + dx);
            int z = MathHelper.floor(player.getZ() + dz);
            int centerY = MathHelper.floor(player.getY());

            BlockPos candidate = findGroundNear(world, x, centerY, z);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos findGroundNear(ServerWorld world, int x, int centerY, int z) {
        for (int dy = 0; dy <= VERTICAL_SEARCH_RANGE; dy++) {
            for (int sign : new int[]{0, 1, -1}) {
                if (dy == 0 && sign != 0) continue;
                int y = centerY + dy * sign;

                BlockPos feet = new BlockPos(x, y, z);
                BlockPos floor = feet.down();

                boolean floorSolid = !world.getBlockState(floor).getCollisionShape(world, floor).isEmpty();
                boolean notInLiquid = world.getFluidState(feet).isEmpty();

                Box body = new Box(
                    x + 0.5 - 0.3, y, z + 0.5 - 0.3,
                    x + 0.5 + 0.3, y + 1.95, z + 0.5 + 0.3
                );
                boolean spaceFree = world.isSpaceEmpty(body);

                if (floorSolid && spaceFree && notInLiquid) {
                    return feet;
                }
            }
        }
        return null;
    }
}