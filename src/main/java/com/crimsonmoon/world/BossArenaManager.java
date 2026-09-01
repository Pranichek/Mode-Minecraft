package com.crimsonmoon.world;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.ModDimensions;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.TeleportTarget;

import java.io.IOException;

/**
 * Измерение боссарены — плоская "пустота" (см. data/crimsonmoon/dimension/boss_realm.json),
 * единственное содержимое которой — структура арены (data/crimsonmoon/structures/boss_arena.nbt),
 * размещаемая вручную кодом (а не генератором мира), один раз, по центру
 * координат (0, {@value #ARENA_BASE_Y}, 0).
 *
 * Структура ОГРОМНАЯ (~4.4 млн блок-записей), поэтому размещается через
 * {@link ArenaStructureLoader} — стриминговый загрузчик, который не держит
 * файл структуры в памяти целиком (в отличие от ванильного
 * {@code StructureTemplateManager}, чей кэш иначе бы навсегда занимал
 * гигабайты heap после первого же захода в арену).
 *
 * Точка высадки игрока ищется автоматически — сканированием вниз от верхней
 * границы структуры по центральной колонне до первого твёрдого блока с двумя
 * свободными клетками над ним. Это не зависит от конкретной планировки
 * арены (не нужно вручную прописывать координаты входа).
 */
public class BossArenaManager {

    private static final String STRUCTURE_RESOURCE_PATH = "/data/crimsonmoon/structures/boss_arena.nbt";

    /** Y, на котором стоит нижняя грань структуры. Небольшой отступ от дна измерения. */
    private static final BlockPos ENTRY_POS = new BlockPos(-100, 4, 1);
    private static final int ARENA_BASE_Y = 1;

    private BossArenaManager() {}

    /** Телепортирует игрока в боссарену, размещая структуру при первом заходе. */
    public static void enter(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerWorld arenaWorld = server.getWorld(ModDimensions.BOSS_REALM);
        if (arenaWorld == null) {
            player.sendMessage(
                Text.literal("Измерение боссарены недоступно (проверь датапак мода).").formatted(Formatting.RED),
                false
            );
            return;
        }

        Vec3i size;
        try {
            size = ensurePlaced(arenaWorld);
        } catch (OutOfMemoryError oom) {
            CrimsonMoon.LOGGER.error("Не хватило памяти при размещении арены", oom);
            player.sendMessage(
                Text.literal("Не хватило памяти при размещении арены — сообщи об этом, это не должно было случиться со стриминговым загрузчиком.")
                    .formatted(Formatting.RED),
                false
            );
            return;
        } catch (IOException | RuntimeException e) {
            CrimsonMoon.LOGGER.error("Не удалось загрузить структуру арены", e);
            player.sendMessage(
                Text.literal("Не удалось загрузить структуру арены — подробности в логе сервера.")
                    .formatted(Formatting.RED),
                false
            );
            return;
        }

        if (size == null) {
            player.sendMessage(
                Text.literal("Не удалось найти структуру арены (data/crimsonmoon/structures/boss_arena.nbt).")
                    .formatted(Formatting.RED),
                false
            );
            return;
        }

        BlockPos landing = ENTRY_POS;

        FabricDimensions.teleport(player, arenaWorld, new TeleportTarget(
            Vec3d.ofBottomCenter(landing), Vec3d.ZERO, player.getYaw(), player.getPitch()
        ));
    }

    /** Возвращает игрока на спавн оверворлда (для команды/выхода из арены). */
    public static void exit(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerWorld overworld = server.getOverworld();
        BlockPos spawn = overworld.getSpawnPos();

        FabricDimensions.teleport(player, overworld, new TeleportTarget(
            Vec3d.ofBottomCenter(spawn), Vec3d.ZERO, player.getYaw(), player.getPitch()
        ));
    }

    /**
     * Размещает структуру арены, если она ещё не размещена (флаг + размер
     * персистентны, переживают перезапуск сервера — см. {@link BossArenaState}).
     * Возвращает размер структуры (нужен для поиска точки высадки) либо null,
     * если файл структуры не найден.
     *
     * ВНИМАНИЕ: первый вызов (когда структуры ещё нет) на пару секунд блокирует
     * серверный поток — это ожидаемо для одноразовой инициализации структуры
     * такого размера. Все последующие входы в арену эту функцию быстро
     * пропускают (флаг уже "placed").
     */
    private static Vec3i ensurePlaced(ServerWorld arenaWorld) throws IOException {
        BossArenaState state = BossArenaState.get(arenaWorld);
        if (state.isPlaced()) {
            return state.getSize();
        }

        byte[] compressedNbt = ArenaStructureLoader.loadResourceBytes(STRUCTURE_RESOURCE_PATH);
        if (compressedNbt == null) {
            return null;
        }

        Vec3i size = ArenaStructureLoader.streamPlace(compressedNbt, arenaWorld, BossArenaManager::arenaOrigin);
        state.markPlaced(size);
        return size;
    }

    /** Угол (минимальная точка) структуры, если центрировать её по X/Z вокруг (0, ARENA_BASE_Y, 0). */
    private static BlockPos arenaOrigin(Vec3i size) {
        return new BlockPos(-size.getX() / 2, ARENA_BASE_Y, -size.getZ() / 2);
    }
}
