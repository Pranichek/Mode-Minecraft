package com.crimsonmoon.world;

import com.crimsonmoon.ModEntities;
import com.crimsonmoon.ModItems;
import com.crimsonmoon.entity.ThorncallerEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.registry.tag.BiomeTags;

/**
 * Thorncaller существует в единственном экземпляре одновременно ("флажок" —
 * {@link #activeThorncallerId}).
 *
 * Спавн: при входе игрока в джунгли, НО только после наложения проклятья
 * (isCurseActive) и пока Thorncaller НЕ УБИТ (ThorncallerBossState.defeated).
 * Если игрок вошёл, не убил и вышел — босс тихо деспавнится, но при следующем
 * заходе в джунгли появится СНОВА (потому что ещё жив по сюжету).
 * После смерти — defeated=true, и больше не появляется никогда.
 */
public class ThorncallerSpawnManager {

    private static final int CHECK_INTERVAL_TICKS = 20; // раз в секунду

    private static final double MIN_DISTANCE = 14.0;
    private static final double MAX_DISTANCE = 20.0;
    private static final double OUT_OF_SIGHT_HALF_ANGLE = 100.0;
    private static final int MAX_PLACEMENT_ATTEMPTS = 16;
    private static final int VERTICAL_SEARCH_RANGE = 5;

    private static final int LOOT_MIN = 1;
    private static final int LOOT_MAX = 3;

    private static int tickCounter = 0;

    /** UUID живого Thorncaller в мире, либо null. */
    private static UUID activeThorncallerId = null;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ThorncallerSpawnManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!server.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        if (activeThorncallerId == null) {
            resyncActiveThorncaller(server);
        }

        List<ServerPlayerEntity> playersInJungle = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isInJungle(player)) {
                playersInJungle.add(player);
            }
        }

        // никто не в джунглях — тихо убираем босса (он не убит, просто "ушёл")
        if (activeThorncallerId != null && playersInJungle.isEmpty()) {
            despawnActive(server);
        }

        // спавн: проклятье активно И босс ещё НЕ УБИТ
        if (activeThorncallerId == null && !playersInJungle.isEmpty()) {

            CrimsonMoonWorldState curse = CrimsonMoonWorldState.getServerState(server);
            ThorncallerBossState bossState = ThorncallerBossState.get(server.getOverworld());

            boolean allowed = curse.isCurseActive
                && !bossState.isDefeated()
                && !bossState.isAwaitingLoot();

            if (allowed) {
                ServerPlayerEntity chosen = playersInJungle.get(
                    server.getOverworld().getRandom().nextInt(playersInJungle.size()));
                trySpawn((ServerWorld) chosen.getWorld(), chosen);
            }
        }
    }

    private static void resyncActiveThorncaller(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof ThorncallerEntity thorncaller && thorncaller.isAlive()) {
                    activeThorncallerId = thorncaller.getUuid();
                    return;
                }
            }
        }
    }

    private static boolean isInJungle(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        return world.getBiome(player.getBlockPos()).isIn(BiomeTags.IS_JUNGLE);
    }

    private static void despawnActive(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(activeThorncallerId);
            if (entity instanceof ThorncallerEntity thorncaller) {
                thorncaller.discard(); // тихо убираем: это не смерть — лут не роняется
                break;
            }
        }
        activeThorncallerId = null;
    }

    private static void trySpawn(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = findOutOfSightPosition(world, player);
        if (pos == null) {
            return;
        }

        if (!world.getBiome(pos).isIn(BiomeTags.IS_JUNGLE)) {
            return;
        }

        ThorncallerEntity thorncaller = ModEntities.THORNCALLER.create(world);
        if (thorncaller == null) {
            return;
        }

        thorncaller.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        world.spawnEntity(thorncaller);

        activeThorncallerId = thorncaller.getUuid();
    }

    /** Вызывается из {@link ThorncallerEntity#onDeath}. */
    public static void onThorncallerDied(ServerWorld world, ThorncallerEntity thorncaller) {
        if (thorncaller.getUuid().equals(activeThorncallerId)) {
            activeThorncallerId = null;
        }

        // ОКОНЧАТЕЛЬНО помечаем убитым — больше не заспавнится никогда
        ThorncallerBossState bossState = ThorncallerBossState.get(world);
        bossState.markLootCollected();

        // дроп лута
        int lootCount = LOOT_MIN + world.getRandom().nextInt(LOOT_MAX - LOOT_MIN + 1);
        ItemStack lootStack = new ItemStack(ModItems.BLOOD_INGOT, lootCount);
        ItemScatterer.spawn(world, thorncaller.getX(), thorncaller.getY() + 0.5, thorncaller.getZ(), lootStack);
    }

    // ==================== Поиск позиции для спавна ====================

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