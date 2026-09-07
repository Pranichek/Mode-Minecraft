package com.crimsonmoon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crimsonmoon.item.NoDropRegistry;
import com.crimsonmoon.item.PendingItemStash;
import com.crimsonmoon.world.ModWorldGen;
import com.crimsonmoon.world.ThorncallerSpawnManager;
import com.crimsonmoon.world.CrimsonMoonWorldState; // ДОБАВЛЕН ИМПОРТ СОСТОЯНИЯ
import com.crimsonmoon.entity.ThorncallerEntity;
import com.crimsonmoon.entity.NightmareEntity;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import dev.emi.trinkets.api.TrinketsApi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.world.Heightmap;

public class CrimsonMoon implements ModInitializer {
    public static final String MOD_ID = "crimsonmoon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();

            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                net.minecraft.scoreboard.Scoreboard sb = server.getScoreboard();
                net.minecraft.scoreboard.ScoreboardObjective obj = sb.getNullableObjective("crimson_monolit");
                if (obj == null) {
                    obj = sb.addObjective(
                        "crimson_monolit",
                        net.minecraft.scoreboard.ScoreboardCriterion.DUMMY,
                        net.minecraft.text.Text.literal("crimson_monolit"),
                        net.minecraft.scoreboard.ScoreboardCriterion.RenderType.INTEGER
                    );
                }
                net.minecraft.scoreboard.ScoreboardPlayerScore score = sb.getPlayerScore("#monolit", obj);

                if (score.getScore() == 0) {
                    spawnMonolitNearPlayer(world, player);
                    score.setScore(1);
                }
            }
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
            !world.getRegistryKey().equals(ModDimensions.BOSS_REALM)
        );

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if(!world.isClient() && state.isOf(ModBlocks.MONOLIT)) {
                ServerWorld serverWorld = (ServerWorld) world;

                for (ServerPlayerEntity target : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                    if (!target.getCommandTags().contains("cursed_player")) {
                        applyCurse(target);
                    }
                }

                serverWorld.setWeather(0, 6000, true, true);
                world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.0f, 0.8f);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean isWorldCursed = false;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getCommandTags().contains("cursed_player")) {
                    isWorldCursed = true;
                    ServerWorld serverWorld = player.getServerWorld();

                    if (serverWorld.random.nextInt(400) == 0) {
                        SoundEvent[] soundEvent = {
                            SoundEvents.ENTITY_CREEPER_PRIMED,
                            SoundEvents.ENTITY_ENDERMAN_STARE,
                            SoundEvents.BLOCK_GLASS_BREAK
                        };
                        SoundEvent randomSound = soundEvent[serverWorld.random.nextInt(soundEvent.length)];
                        player.playSound(randomSound, SoundCategory.HOSTILE, 1.0f, 1.0f);
                    }

                    if (serverWorld.random.nextInt(20) == 0){
                        BlockPos playerPos = player.getBlockPos();
                        int radius = 4;
                        BlockPos targetPos = playerPos.add(
                            serverWorld.random.nextInt(radius * 2 ) - radius,
                            serverWorld.random.nextInt(radius * 2 ) - radius,
                            serverWorld.random.nextInt(radius * 2 ) - radius
                        );

                        BlockState targetState = serverWorld.getBlockState(targetPos);

                        if(targetState.isOf(Blocks.TORCH) || targetState.isOf(Blocks.WALL_TORCH)){
                            serverWorld.breakBlock(targetPos, false);
                            serverWorld.playSound(null, targetPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f);
                            serverWorld.spawnParticles(ParticleTypes.SMOKE, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 10, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
            }

            if (isWorldCursed) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (!player.getCommandTags().contains("cursed_player")) {
                        applyCurse(player);
                    }
                }
            }

            if (isWorldCursed) {
                ServerWorld overworld = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (overworld != null && !overworld.isRaining()) {
                    overworld.setWeather(0, 1000000, true, true);
                }
            }

            // --- Глобальная логика одноразового спавна босса ---
            if (server.getTicks() % 100 == 0) {
                CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(server);

                if (state.isCurseActive && !state.nightmareSpawned && !state.nightmareDefeated) {
                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                        if (p.getY() < 55 && p.getCommandTags().contains("cursed_player")) {
                            ServerWorld sWorld = p.getServerWorld();
                            BlockPos cavePos = findSpaciousCave(sWorld, p.getBlockPos(), 20);

                            if (cavePos != null) {
                                NightmareEntity nightmare = ModEntities.NIGHTMARE.create(sWorld);
                                if (nightmare != null) {
                                    nightmare.refreshPositionAndAngles(cavePos.getX() + 0.5, cavePos.getY(), cavePos.getZ() + 0.5, sWorld.random.nextFloat() * 360F, 0.0F);
                                    sWorld.spawnEntity(nightmare);

                                    state.nightmareSpawned = true;
                                    state.markDirty();
                                    break; // только один босс
                                }
                            }
                        }
                    }
                }
            }
            // ---------------------------------------------------
        });

        ModItemGroups.registerItemGroups();
        ModSounds.register();
        ModBlocks.registerModBlocks();
        ModEntities.registerModEntities();
        ModWorldGen.generateModWorldGen();
        com.crimsonmoon.world.WatcherManager.register();
        ThorncallerSpawnManager.register();
        NoDropRegistry.protect(ModItems.BLOOD_MONOLIT);

        BossArenaCommand.register();
        com.crimsonmoon.world.MonsterikBossManager.register();
        MonsterikBossCommand.register();
        FearManager.register();

        ThorncallerEntity.MINION_TYPE = ModEntities.BLOODWALKER;

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            for (ItemStack stack : PendingItemStash.take(oldPlayer.getUuid())) {
                if (!newPlayer.getInventory().insertStack(stack)) {
                    newPlayer.dropItem(stack, false);
                }
            }
        });

        ParanoiaSystem.register();
        ModPotions.register();
        ArmorSetEffectManager.register();

        BrewingRecipeRegistry.registerPotionRecipe(Potions.STRENGTH, ModItems.BLOOD_INGOT, ModPotions.BATTLE_POTION);
        BrewingRecipeRegistry.registerPotionRecipe(Potions.SWIFTNESS, ModItems.BLOOD_INGOT, ModPotions.BERSERKER_POTION);
        BrewingRecipeRegistry.registerPotionRecipe(Potions.REGENERATION, ModItems.BLOOD_INGOT, ModPotions.HEROIC_POTION);
    }

    private static void applyCurse(ServerPlayerEntity target) {
        target.addCommandTag("cursed_player");

        if (target.getServer() != null) {
            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(target.getServer());
            if (!state.isCurseActive) {
                state.isCurseActive = true;
                state.markDirty();
            }
        }

        TrinketsApi.getTrinketComponent(target).ifPresent(trinkets -> {
            trinkets.getInventory().get("chest").get("necklace").setStack(0, new ItemStack(ModItems.BLOOD_NECKLACE));
        });

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0, false, false, true));
        target.sendMessage(Text.literal("Прокляття спостерігача пробудилося!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
    }

    private static void spawnMonolitNearPlayer(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        Identifier structureId = new Identifier(MOD_ID, "crimsonmoon");

        var templateOpt = world.getStructureTemplateManager().getTemplate(structureId);
        if (templateOpt.isEmpty()) {
            return;
        }
        net.minecraft.structure.StructureTemplate template = templateOpt.get();

        net.minecraft.util.math.Vec3i size = template.getSize();
        int sizeX = Math.max(1, size.getX());
        int sizeZ = Math.max(1, size.getZ());

        final int STEP = 2;
        final int RING_WIDTH = 60;
        final int MAX_DIST = 1000;
        final int TRIES_PER_RING = 250;
        final int GOOD_ENOUGH = 2;

        BlockPos landOrigin = null;
        int landGroundY = 0;
        int landFlatness = Integer.MAX_VALUE;

        BlockPos anyOrigin = null;
        int anyGroundY = 0;

        int minDist = 20;
        while (minDist < MAX_DIST) {
            int maxDist = Math.min(minDist + RING_WIDTH, MAX_DIST);

            for (int attempt = 0; attempt < TRIES_PER_RING; attempt++) {
                int dist = minDist + world.random.nextInt(maxDist - minDist + 1);
                double angle = world.random.nextDouble() * Math.PI * 2.0;
                int ox = playerPos.getX() + (int) (Math.cos(angle) * dist);
                int oz = playerPos.getZ() + (int) (Math.sin(angle) * dist);

                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                boolean allLand = true;

                for (int lx = 0; lx <= sizeX; lx += STEP) {
                    for (int lz = 0; lz <= sizeZ; lz += STEP) {
                        int wx = ox + lx;
                        int wz = oz + lz;

                        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                        BlockState ground = world.getBlockState(new BlockPos(wx, y - 1, wz));

                        boolean natural = ground.isOf(Blocks.GRASS_BLOCK) || ground.isOf(Blocks.DIRT)
                            || ground.isOf(Blocks.STONE) || ground.isOf(Blocks.SAND)
                            || ground.isOf(Blocks.PODZOL) || ground.isOf(Blocks.SNOW_BLOCK)
                            || ground.isOf(Blocks.GRAVEL) || ground.isOf(Blocks.COARSE_DIRT)
                            || ground.isOf(Blocks.MOSS_BLOCK) || ground.isOf(Blocks.MYCELIUM)
                            || ground.isOf(Blocks.ROOTED_DIRT) || ground.isOf(Blocks.CLAY)
                            || ground.isOf(Blocks.SANDSTONE) || ground.isOf(Blocks.TERRACOTTA);

                        if (!ground.getFluidState().isEmpty() || !natural) {
                            allLand = false;
                        }

                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }

                if (anyOrigin == null) {
                    anyOrigin = new BlockPos(ox, minY, oz);
                    anyGroundY = minY;
                }

                if (!allLand) continue;

                int flatness = maxY - minY;
                if (flatness < landFlatness) {
                    landFlatness = flatness;
                    landOrigin = new BlockPos(ox, minY, oz);
                    landGroundY = minY;
                }

                if (flatness <= GOOD_ENOUGH) {
                    placeMonolit(world, template, landOrigin, landGroundY);
                    return;
                }
            }

            minDist = maxDist;
        }

        if (landOrigin != null) {
            placeMonolit(world, template, landOrigin, landGroundY);
            return;
        }

        BlockPos fallback = (anyOrigin != null)
            ? anyOrigin
            : new BlockPos(playerPos.getX() + 40, world.getSeaLevel(), playerPos.getZ());
        int platformY = (anyOrigin != null) ? anyGroundY : world.getSeaLevel();

        buildPlatform(world, fallback, platformY, sizeX, sizeZ);
        placeMonolit(world, template, fallback, platformY);
    }

    private static void buildPlatform(ServerWorld world, BlockPos origin, int groundY, int sizeX, int sizeZ) {
        int margin = 2;
        for (int lx = -margin; lx <= sizeX + margin; lx++) {
            for (int lz = -margin; lz <= sizeZ + margin; lz++) {
                int wx = origin.getX() + lx;
                int wz = origin.getZ() + lz;

                world.setBlockState(new BlockPos(wx, groundY - 1, wz), Blocks.STONE.getDefaultState());
                world.setBlockState(new BlockPos(wx, groundY - 2, wz), Blocks.STONE.getDefaultState());
                world.setBlockState(new BlockPos(wx, groundY - 3, wz), Blocks.STONE.getDefaultState());

                for (int up = 0; up < 6; up++) {
                    BlockPos clear = new BlockPos(wx, groundY + up, wz);
                    if (!world.getBlockState(clear).getFluidState().isEmpty()) {
                        world.setBlockState(clear, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private static void placeMonolit(ServerWorld world,
            net.minecraft.structure.StructureTemplate template,
            BlockPos origin, int groundY) {
        BlockPos finalSpawnPos = new BlockPos(origin.getX(), groundY - 1, origin.getZ());

        StructurePlacementData placementData = new StructurePlacementData()
            .setRotation(BlockRotation.NONE)
            .setMirror(BlockMirror.NONE)
            .setIgnoreEntities(false);

        template.place(world, finalSpawnPos, finalSpawnPos, placementData, world.random, 2);
    }

    private static BlockPos findSpaciousCave(ServerWorld world, BlockPos center, int radius) {
        // 1) Ищем готовое место: твёрдый пол + 2 клетки воздуха
        for (int i = 0; i < 80; i++) {
            BlockPos checkPos = center.add(
                world.random.nextInt(radius * 2) - radius,
                world.random.nextInt(16) - 8,
                world.random.nextInt(radius * 2) - radius
            );
            if (checkPos.isWithinDistance(center, 3.0)) continue;

            BlockPos floor = checkPos.down();
            if (world.getBlockState(floor).isSolidBlock(world, floor)
                    && world.getBlockState(checkPos).isAir()
                    && world.getBlockState(checkPos.up()).isAir()) {
                return checkPos;
            }
        }

        // 2) Готового не нашлось — берём точку рядом с игроком и ВЫКАПЫВАЕМ место под босса.
        //    Босс всё равно сам расчищает вокруг себя (freeSpaceAround), так что появится нормально.
        int dx = world.random.nextInt(7) - 3; // 3 блока в сторону
        int dz = world.random.nextInt(7) - 3;
        BlockPos spot = center.add(dx, 0, dz);

        world.setBlockState(spot, Blocks.AIR.getDefaultState());
        world.setBlockState(spot.up(), Blocks.AIR.getDefaultState());
        BlockPos floor = spot.down();
        if (!world.getBlockState(floor).isSolidBlock(world, floor)) {
            world.setBlockState(floor, Blocks.STONE.getDefaultState());
        }
        return spot;
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}