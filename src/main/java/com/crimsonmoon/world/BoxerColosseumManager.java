package com.crimsonmoon.world;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.ModEntities;
import com.crimsonmoon.entity.BoxerEntity;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.Optional;

/**
 * После проклятья находит ближайшую пустыню и САМ расчищает/выравнивает там
 * площадку под колизей — без привередливых проверок. Место всегда «подходящее»,
 * потому что мы его сами делаем ровным. Ставит колизей + боксёра, координаты в state.
 */
public class BoxerColosseumManager {

    private static final Identifier COLOSSEUM_ID = new Identifier(CrimsonMoon.MOD_ID, "boxer_colosseum");

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 100 != 0) return; // раз в 5 сек

            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(server);
            if (state.boxerArenaPlaced) return;
            if (!state.isCurseActive) return;

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!p.getCommandTags().contains("cursed_player")) continue;

                ServerWorld world = p.getServerWorld();
                if (world.getRegistryKey() != net.minecraft.world.World.OVERWORLD) continue;

                CrimsonMoon.LOGGER.info("[BOXER] Ищу ближайшую пустыню от {}", p.getBlockPos());
                placeInNearestDesert(world, p.getBlockPos(), state);
                break;
            }
        });
    }

    private static void placeInNearestDesert(ServerWorld world, BlockPos from, CrimsonMoonWorldState state) {
        Optional<StructureTemplate> templateOpt = world.getStructureTemplateManager().getTemplate(COLOSSEUM_ID);
        if (templateOpt.isEmpty()) {
            CrimsonMoon.LOGGER.warn("[BOXER] Не найдена структура колизея: {}", COLOSSEUM_ID);
            return;
        }
        StructureTemplate template = templateOpt.get();
        Vec3i size = template.getSize();

        // ищем пустыню, расширяя радиус
        BlockPos desert = null;
        for (int radius = 3000; radius <= 12000 && desert == null; radius += 3000) {
            Pair<BlockPos, RegistryEntry<Biome>> found = world.locateBiome(
                b -> b.matchesKey(BiomeKeys.DESERT), from, radius, 32, 64);
            if (found != null) {
                desert = found.getFirst();
                CrimsonMoon.LOGGER.info("[BOXER] Пустыня найдена в радиусе {} на {}", radius, desert);
            }
        }

        if (desert == null) {
            CrimsonMoon.LOGGER.warn("[BOXER] Пустыня не найдена даже в 12000 блоков — попробуем позже");
            return;
        }

        int cx = desert.getX();
        int cz = desert.getZ();

        // ПРИНУДИТЕЛЬНО грузим чанк центра, чтобы высота была честной (не под землёй)
        world.getChunk(cx >> 4, cz >> 4);

        // берём высоту поверхности в центре как базовый уровень пола арены
        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx, cz) - 1;
        if (baseY <= world.getBottomY() + 2) {
            baseY = 64; // страховка от кривой высоты
        }

        CrimsonMoon.LOGGER.info("[BOXER] Расчищаю площадку в центре {} на высоте {}", desert, baseY);

        // САМИ расчищаем и выравниваем площадку под весь колизей (+запас)
        prepareGround(world, cx, cz, size.getX(), size.getZ(), baseY);

        // ставим колизей на подготовленный пол
        BlockPos origin = new BlockPos(cx - size.getX() / 2, baseY + 1, cz - size.getZ() / 2);
        StructurePlacementData data = new StructurePlacementData()
            .setRotation(BlockRotation.NONE)
            .setMirror(BlockMirror.NONE)
            .setIgnoreEntities(true);
        template.place(world, origin, origin, data, world.random, 2);

        // центр арены + босс
        BlockPos arenaCenter = new BlockPos(cx, baseY + 14, cz);
        state.arenaX = arenaCenter.getX();
        state.arenaY = arenaCenter.getY();
        state.arenaZ = arenaCenter.getZ();
        state.boxerArenaPlaced = true;

        BoxerEntity boxer = ModEntities.BOXER.create(world);
        if (boxer != null) {
            boxer.refreshPositionAndAngles(
                arenaCenter.getX() + 0.5, arenaCenter.getY(), arenaCenter.getZ() + 0.5,
                world.random.nextFloat() * 360F, 0F);
            world.spawnEntity(boxer);
            state.boxerSpawned = true;
        }

        state.markDirty();
        CrimsonMoon.LOGGER.info("[BOXER] Колизей поставлен на {}", arenaCenter);
    }

    /**
     * Расчищает и выравнивает площадку под колизей:
     *  - под полом кладёт песчаник (опора, чтобы не висел над ямой/пещерой),
     *  - на уровне пола кладёт песок,
     *  - всё выше пола (на высоту колизея) вычищает в воздух (срезает дюны/кактусы/что угодно).
     * Площадка чуть больше структуры — с запасом по краям.
     */
    private static void prepareGround(ServerWorld world, int cx, int cz, int sizeX, int sizeZ, int baseY) {
        int halfX = sizeX / 2 + 4;
        int halfZ = sizeZ / 2 + 4;
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -halfZ; dz <= halfZ; dz++) {
                int x = cx + dx;
                int z = cz + dz;

                // грузим чанк перед изменением (важно для далёкой пустыни)
                world.getChunk(x >> 4, z >> 4);

                // опора под полом — 4 блока песчаника
                for (int d = 1; d <= 4; d++) {
                    m.set(x, baseY - d, z);
                    world.setBlockState(m, Blocks.SANDSTONE.getDefaultState(), 2);
                }

                // пол — песок
                m.set(x, baseY, z);
                world.setBlockState(m, Blocks.SAND.getDefaultState(), 2);

                // всё выше пола расчищаем (высота с запасом под колизей)
                for (int up = 1; up <= 45; up++) {
                    m.set(x, baseY + up, z);
                    if (!world.getBlockState(m).isAir()) {
                        world.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private BoxerColosseumManager() {}
}