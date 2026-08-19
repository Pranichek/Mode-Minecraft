package com.crimsonmoon.world;

import com.crimsonmoon.ModEntities;
import com.crimsonmoon.ModSounds;
import com.crimsonmoon.entity.WatcherEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WatcherManager {

    private static final int CHECK_INTERVAL_TICKS = 3 * 60 * 20; // 3 минуты — можно менять
    private static final float SPAWN_CHANCE = 0.45f;              // 45% — можно менять

    private static final double MIN_DISTANCE = 24.0;
    private static final double MAX_DISTANCE = 28.0;
    private static final double FRONT_CONE_HALF_ANGLE = 100.0; // сколько градусов спереди исключаем
    private static final int MAX_PLACEMENT_ATTEMPTS = 20;
    private static final int VERTICAL_SEARCH_RANGE = 6;

    private static final Map<UUID, Integer> timers = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(WatcherManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            int ticks = timers.merge(id, 1, Integer::sum);

            if (ticks < CHECK_INTERVAL_TICKS) {
                continue;
            }

            timers.put(id, 0);

            if (player.getRandom().nextFloat() >= SPAWN_CHANCE) {
                continue; // не повезло в этот раз
            }

            trySpawnWatcher((ServerWorld) player.getWorld(), player);
        }
    }

    private static void trySpawnWatcher(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = findSpawnPosition(world, player);
        if (pos == null) {
            return; // безопасного места не нашли — просто пропускаем эту попытку
        }

        WatcherEntity watcher = ModEntities.WATCHER.create(world);
        if (watcher == null) {
            return;
        }

        watcher.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        world.spawnEntity(watcher);
        watcher.setOwner(player);

        player.playSound(ModSounds.WATCHER_THEME, SoundCategory.AMBIENT, 1.0f, 1.0f);
    }

    private static BlockPos findSpawnPosition(ServerWorld world, ServerPlayerEntity player) {
        float playerYawRad = (float) Math.toRadians(MathHelper.wrapDegrees(player.getYaw()));

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            double angleOffsetDeg = FRONT_CONE_HALF_ANGLE
                + player.getRandom().nextDouble() * (360.0 - 2 * FRONT_CONE_HALF_ANGLE);
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

    // ищем твёрдый пол + 2 блока воздуха, ближе всего по высоте к игроку —
    // именно это спасает от спавна внутри стены пещеры
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