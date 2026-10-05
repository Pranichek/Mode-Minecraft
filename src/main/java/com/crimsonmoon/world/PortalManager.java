package com.crimsonmoon.world;

import com.crimsonmoon.CrimsonMoon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

import java.util.Optional;

/**
 * После победы над ТРЕМЯ пред-боссами (Nightmare, Boxer, Thorncaller) один раз
 * ставит портал далеко (~1000+ блоков) от игрока. Портал — структура portal.nbt.
 */
public class PortalManager {

    private static final Identifier PORTAL_ID = new Identifier(CrimsonMoon.MOD_ID, "portal");

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 100 != 0) return; // раз в 5 сек

            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(server);
            if (state.portalPlaced) return;

            // все три пред-босса должны быть убиты
            ThorncallerBossState thorn = ThorncallerBossState.get(server.getOverworld());
            boolean allThree = state.nightmareDefeated && state.boxerDefeated && thorn.isDefeated();
            if (!allThree) return;

            // точка отсчёта — любой игрок
            ServerPlayerEntity anyPlayer = null;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                anyPlayer = p;
                break;
            }
            if (anyPlayer == null) return;

            ServerWorld world = server.getOverworld();
            placePortal(world, anyPlayer.getBlockPos(), state);
        });
    }

    private static void placePortal(ServerWorld world, BlockPos from, CrimsonMoonWorldState state) {
        Optional<StructureTemplate> templateOpt = world.getStructureTemplateManager().getTemplate(PORTAL_ID);
        if (templateOpt.isEmpty()) {
            CrimsonMoon.LOGGER.warn("[PORTAL] Не найдена структура портала: {}", PORTAL_ID);
            return;
        }
        StructureTemplate template = templateOpt.get();
        Vec3i size = template.getSize();

        // ищем место в 1000..1600 блоков от игрока, в случайном направлении
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            int dist = 1000 + world.random.nextInt(600);
            int cx = from.getX() + (int) (Math.cos(angle) * dist);
            int cz = from.getZ() + (int) (Math.sin(angle) * dist);

            // ПРИНУДИТЕЛЬНО грузим чанк — иначе высота будет неверной (портал под землёй)
            world.getChunk(cx >> 4, cz >> 4);

            int cy = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx, cz);
            BlockPos ground = new BlockPos(cx, cy - 1, cz);

            BlockState groundState = world.getBlockState(ground);
            // место годится: твёрдый пол, не вода/лёд
            if (!groundState.isSolidBlock(world, ground)) continue;
            if (groundState.isOf(Blocks.WATER) || groundState.isOf(Blocks.ICE)) continue;
            // над поверхностью не должно быть жидкости
            if (!world.getFluidState(new BlockPos(cx, cy, cz)).isEmpty()) continue;

            BlockPos origin = new BlockPos(cx - size.getX() / 2, cy, cz - size.getZ() / 2);
            StructurePlacementData data = new StructurePlacementData()
                .setRotation(BlockRotation.NONE)
                .setMirror(BlockMirror.NONE)
                .setIgnoreEntities(true);
            template.place(world, origin, origin, data, world.random, 2);

            // центр портала — сюда игрок будет идти, у этой точки активируем ключ
            state.portalPlaced = true;
            state.portalX = cx;
            state.portalY = cy;
            state.portalZ = cz;
            state.markDirty();

            CrimsonMoon.LOGGER.info("[PORTAL] Портал поставлен на {}, {}, {}", cx, cy, cz);
            return;
        }

        CrimsonMoon.LOGGER.warn("[PORTAL] Не нашли место для портала — попробуем позже");
    }

    private PortalManager() {}
}