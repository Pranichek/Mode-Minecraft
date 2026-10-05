package com.crimsonmoon.world;

import com.crimsonmoon.ModDimensions;
import com.crimsonmoon.ModEntities;
import com.crimsonmoon.entity.MonsterikEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Monsterik существует в единственном экземпляре одновременно во всём мире:
 *
 *  1. {@link #register()} вешает периодическую проверку на тик сервера —
 *     если сейчас на арене нет живого Monsterik, он спавнится сам заново в
 *     {@link #ARENA_SPAWN_POS}. Так он появляется сам при старте сервера.
 *     Но это работает только ДО его первой смерти — как только его убили
 *     хотя бы один раз, автоспавн навсегда отключается
 *     ({@link #autoRespawnEnabled}), и дальше он появляется ТОЛЬКО через
 *     команду {@code /monsterikboss spawn}.
 *  2. Тот же {@link #register()} вешает обработчик на загрузку ЛЮБОЙ
 *     сущности (в т.ч. заспавненной вручную командой /summon) — если это
 *     уже второй живой Monsterik, лишний экземпляр тихо удаляется. Это
 *     гарантирует единственность независимо от способа спавна.
 *  3. От естественного деспавна (когда рядом долго нет игроков) сам
 *     Monsterik защищён в коде сущности — см.
 *     {@link MonsterikEntity#canImmediatelyDespawn(double)}.
 *
 * Спавн происходит фиксированно на арене — в измерении
 * {@link ModDimensions#BOSS_REALM}, в точке {@link #ARENA_SPAWN_POS}.
 */
public final class MonsterikBossManager {

    /** Координаты арены, где должен спавниться босс. */
    public static final BlockPos ARENA_SPAWN_POS = new BlockPos(39, 6, 0);

    /** Как часто проверять необходимость автоспавна. */
    private static final int CHECK_INTERVAL_TICKS = 100; // раз в 5 секунд

    private static int tickCounter = 0;

    /** UUID живого Monsterik в мире, либо null. */
    private static UUID activeMonsterikId = null;

    /**
     * Разрешён ли автоспавн (без команды). Изначально true — Monsterik
     * появляется сам при старте сервера. Как только он умер хотя бы один
     * раз, становится false навсегда (до перезапуска сервера) — дальше
     * только вручную через {@code /monsterikboss spawn}.
     */
    private static boolean autoRespawnEnabled = true;

    private MonsterikBossManager() {}

    public static void register() {
        // Автоспавн/поддержание единственного экземпляра — раз в CHECK_INTERVAL_TICKS.
        ServerTickEvents.END_SERVER_TICK.register(MonsterikBossManager::onServerTick);

        // Ловим загрузку/спавн ЛЮБОГО Monsterik (в т.ч. через /summon) и не
        // допускаем существования второго экземпляра одновременно.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof MonsterikEntity monsterik) || !entity.isAlive()) {
                return;
            }

            if (activeMonsterikId == null) {
                activeMonsterikId = monsterik.getUuid();
                return;
            }

            if (!activeMonsterikId.equals(monsterik.getUuid())
                    && isStillAlive(world.getServer(), activeMonsterikId)) {
                // уже есть живой Monsterik — лишний тихо убираем
                monsterik.discard();
            } else {
                activeMonsterikId = monsterik.getUuid();
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        if (!autoRespawnEnabled) {
            return;
        }

        resyncActiveMonsterik(server);
        if (activeMonsterikId != null) {
            return;
        }

        ServerWorld arenaWorld = server.getWorld(ModDimensions.BOSS_REALM);
        if (arenaWorld == null) {
            return;
        }

        spawnAt(arenaWorld);
    }

    public enum SpawnResult {
        SPAWNED,
        ALREADY_ALIVE,
        DIMENSION_UNAVAILABLE
    }

    /** Спавнит Monsterik на арене немедленно (для {@code /monsterikboss spawn}). */
    public static SpawnResult trySpawn(MinecraftServer server) {
        resyncActiveMonsterik(server);

        if (activeMonsterikId != null) {
            return SpawnResult.ALREADY_ALIVE;
        }

        ServerWorld arenaWorld = server.getWorld(ModDimensions.BOSS_REALM);
        if (arenaWorld == null) {
            return SpawnResult.DIMENSION_UNAVAILABLE;
        }

        MonsterikEntity monsterik = spawnAt(arenaWorld);
        return monsterik != null ? SpawnResult.SPAWNED : SpawnResult.DIMENSION_UNAVAILABLE;
    }

    private static MonsterikEntity spawnAt(ServerWorld arenaWorld) {
        MonsterikEntity monsterik = ModEntities.MONSTERIK.create(arenaWorld);
        if (monsterik == null) {
            return null;
        }

        monsterik.refreshPositionAndAngles(
            ARENA_SPAWN_POS.getX() + 0.5,
            ARENA_SPAWN_POS.getY(),
            ARENA_SPAWN_POS.getZ() + 0.5,
            0.0f, 0.0f
        );

        arenaWorld.spawnEntity(monsterik);
        activeMonsterikId = monsterik.getUuid();
        return monsterik;
    }

    /** Принудительно убирает активного Monsterik (для {@code /monsterikboss kill}). Возвращает true, если кто-то был убран. */
    public static boolean killActive(MinecraftServer server) {
        resyncActiveMonsterik(server);
        if (activeMonsterikId == null) {
            return false;
        }

        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(activeMonsterikId);
            if (entity instanceof MonsterikEntity monsterik) {
                monsterik.discard();
                break;
            }
        }
        activeMonsterikId = null;
        return true;
    }

    public static boolean isActive(MinecraftServer server) {
        resyncActiveMonsterik(server);
        return activeMonsterikId != null;
    }

    /** Вызывается из {@link MonsterikEntity#onDeath}. Навсегда отключает автоспавн — дальше только через команду. */
    public static void onMonsterikDied(ServerWorld world, MonsterikEntity monsterik) {
        if (monsterik.getUuid().equals(activeMonsterikId)) {
            activeMonsterikId = null;
        }
        autoRespawnEnabled = false;
    }

    /** На случай перезапуска сервера, пока Monsterik был жив — подхватываем его заново. */
    private static void resyncActiveMonsterik(MinecraftServer server) {
        if (activeMonsterikId != null && isStillAlive(server, activeMonsterikId)) {
            return;
        }
        activeMonsterikId = null;

        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof MonsterikEntity monsterik && monsterik.isAlive()) {
                    activeMonsterikId = monsterik.getUuid();
                    return;
                }
            }
        }
    }

    private static boolean isStillAlive(MinecraftServer server, UUID id) {
        if (server == null || id == null) {
            return false;
        }
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof MonsterikEntity monsterik && monsterik.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
