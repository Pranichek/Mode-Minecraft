package com.crimsonmoon;

import com.crimsonmoon.entity.BloodwalkerEntity;
import com.crimsonmoon.entity.MonsterikEntity;
import com.crimsonmoon.entity.NightmareEntity;
import com.crimsonmoon.entity.ThorncallerEntity;
import com.crimsonmoon.entity.WatcherEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Шкала страха как игровой ресурс ("заряд").
 *
 * Заполнение НАМЕРЕННО медленное (в очках/тик, а не мгновенное "%
 * от дистанции", как было раньше) и требует реальной угрозы:
 *  - поблизости (в радиусе {@link #FEAR_SOURCE_RADIUS}) есть один из пугающих
 *    мобов мода (Thorncaller, Bloodwalker, Nightmare, Watcher, Monsterik) —
 *    чем ближе, тем быстрее растёт страх;
 *  - и/или игрок стоит в темноте;
 *  - и/или игрок находится на боссарене ({@code ModDimensions.BOSS_REALM}) —
 *    там страх копится гарантированно быстро (см. {@code ARENA_FEAR_GAIN_PER_TICK}),
 *    независимо от прочих условий, иначе способностям посоха/булавы там
 *    попросту нечем питаться.
 * Когда угроз нет, шкала медленно затухает сама.
 *
 * Реальное значение синхронизируется на клиент по сети (см. {@link #SYNC_CHANNEL}
 * и {@code FearHudOverlay}) — HUD у игрока показывает именно то, что хранится
 * здесь, а не считает страх самостоятельно.
 */
public class FearManager {

    public static final float MAX_FEAR = 100.0f;

    /** Канал, по которому серверное (авторитетное) значение страха рассылается владельцу для HUD. */
    public static final Identifier SYNC_CHANNEL = CrimsonMoon.id("fear_sync");

    private static final double FEAR_SOURCE_RADIUS = 16.0;
    private static final int DARK_LIGHT_THRESHOLD = 7;

    // Сознательно небольшие числа — шкала наполняется намного медленнее и
    // по другому условию, чем раньше (раньше это был мгновенный пересчёт
    // дистанции без всякого "тика"). При этом стоимость выстрела/способностей
    // рассчитана так, чтобы во время реального боя рядом с источником страха
    // шкала успевала восстанавливаться, а не превращалась в ресурс "на один раз".
    private static final float PROXIMITY_GAIN_PER_TICK = 0.22f; // на минимальной дистанции до источника
    private static final float DARKNESS_GAIN_PER_TICK = 0.05f;
    private static final float SAFE_DECAY_PER_TICK = 0.02f;

    // На боссарене страх копится гарантированно и быстро (сам факт нахождения
    // на арене — уже угроза), независимо от дистанции до Monsterik и уровня
    // освещения — иначе способности посоха/булавы там банально нечем питать.
    private static final float ARENA_FEAR_GAIN_PER_TICK = 2.0f;

    private static final Map<UUID, Float> fear = new HashMap<>();

    private FearManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
    }

    public static float get(PlayerEntity player) {
        return fear.getOrDefault(player.getUuid(), 0.0f);
    }

    public static float getPercent(PlayerEntity player) {
        return MathHelper.clamp(get(player) / MAX_FEAR, 0.0f, 1.0f);
    }

    /** Пытается потратить {@code amount} страха. Возвращает true, если хватило и трата прошла. */
    public static boolean consume(PlayerEntity player, float amount) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        float current = get(player);
        if (current + 1.0e-4f < amount) {
            return false;
        }
        set(player.getUuid(), current - amount);
        return true;
    }

    public static boolean hasAtLeast(PlayerEntity player, float amount) {
        return player.isCreative() || player.isSpectator() || get(player) >= amount;
    }

    private static void set(UUID id, float value) {
        fear.put(id, MathHelper.clamp(value, 0.0f, MAX_FEAR));
    }

    private static void tick(ServerPlayerEntity player) {
        World world = player.getWorld();
        float current = get(player);

        double closest = closestFearSourceDistance(player);
        boolean inDarkness = world.getLightLevel(LightType.BLOCK, player.getBlockPos()) <= DARK_LIGHT_THRESHOLD
            && world.getLightLevel(LightType.SKY, player.getBlockPos()) <= DARK_LIGHT_THRESHOLD;
        boolean onBossArena = world.getRegistryKey() == com.crimsonmoon.ModDimensions.BOSS_REALM;

        float gain = 0.0f;
        if (closest <= FEAR_SOURCE_RADIUS) {
            double proximity = 1.0 - (closest / FEAR_SOURCE_RADIUS);
            gain += (float) (PROXIMITY_GAIN_PER_TICK * proximity);
        }
        if (inDarkness) {
            gain += DARKNESS_GAIN_PER_TICK;
        }
        if (onBossArena) {
            gain += ARENA_FEAR_GAIN_PER_TICK;
        }

        if (gain > 0.0f) {
            set(player.getUuid(), current + gain);
        } else if (current > 0.0f) {
            set(player.getUuid(), current - SAFE_DECAY_PER_TICK);
        }

        // рассылаем игроку АКТУАЛЬНОЕ значение (в т.ч. уже уменьшенное тратами
        // на способности, которые могли произойти этим же тиком раньше) —
        // HUD у игрока больше не считает страх сам по себе, а просто
        // отображает то, что реально хранится на сервере.
        sendSync(player);
    }

    private static void sendSync(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(get(player));
        ServerPlayNetworking.send(player, SYNC_CHANNEL, buf);
    }

    private static double closestFearSourceDistance(ServerPlayerEntity player) {
        Box searchBox = player.getBoundingBox().expand(FEAR_SOURCE_RADIUS);
        List<Entity> sources = player.getWorld().getOtherEntities(player, searchBox, FearManager::isFearSource);

        double closest = Double.MAX_VALUE;
        for (Entity source : sources) {
            double dist = player.distanceTo(source);
            if (dist < closest) {
                closest = dist;
            }
        }
        return closest;
    }

    private static boolean isFearSource(Entity entity) {
        return entity instanceof ThorncallerEntity
            || entity instanceof BloodwalkerEntity
            || entity instanceof NightmareEntity
            || entity instanceof WatcherEntity
            || entity instanceof MonsterikEntity;
    }
}
