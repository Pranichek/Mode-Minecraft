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

// Импорты для структур
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.world.Heightmap;

public class CrimsonMoon implements ModInitializer {
    public static final String MOD_ID = "crimsonmoon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        
        // --- 1. БЕЗОПАСНЫЙ СПАВН СТРУКТУРЫ ПРИ ПЕРВОМ ЗАХОДЕ ---
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();
            
            // Проверяем, что это обычный мир, а игрок еще не видел структуру
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                if (!player.getCommandTags().contains("monolit_spawned")) {
                    player.addCommandTag("monolit_spawned");
                    spawnMonolitNearPlayer(world, player.getBlockPos());
                }
            }
        });

        // --- 2. ОТСЛЕЖИВАНИЕ РАЗРУШЕНИЯ МОНОЛИТА ---
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if(!world.isClient() && state.isOf(ModBlocks.MONOLIT)) {
                if (!player.getCommandTags().contains("cursed_player")){
                    player.addCommandTag("cursed_player");
                    
                    // Надеваем ожерелье в слот шеи
                    TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
                        trinkets.getInventory().get("chest").get("necklace").setStack(0, new ItemStack(ModItems.BLOOD_NECKLACE));
                    });
                    
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.setWeather(0, 6000, true, true);
                    
                    player.sendMessage(Text.literal("Прокляття спостерігача пробудилося!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                    
                    world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.0f, 0.8f);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0, false, false, true));
                }
            }
        });

        // --- 3. ЭФФЕКТЫ ПРОКЛЯТИЯ И БОСС ---
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean isWorldCursed = false;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getCommandTags().contains("cursed_player")) {
                    isWorldCursed = true;
                    ServerWorld serverWorld = player.getServerWorld();
                    
                    // Случайные жуткие звуки
                    if (serverWorld.random.nextInt(400) == 0) {
                        SoundEvent[] soundEvent = {
                            SoundEvents.ENTITY_CREEPER_PRIMED,
                            SoundEvents.ENTITY_ENDERMAN_STARE,
                            SoundEvents.BLOCK_GLASS_BREAK
                        };
                        SoundEvent randomSound = soundEvent[serverWorld.random.nextInt(soundEvent.length)];
                        player.playSound(randomSound, SoundCategory.HOSTILE, 1.0f, 1.0f);
                    }

                    // Потухание факелов
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

                    // Шанс спавна босса Nightmare в пещерах
                    if (serverWorld.random.nextInt(1500) == 0) { 
                        BlockPos cavePos = findSpaciousCave(serverWorld, player.getBlockPos(), 20);
                        if (cavePos != null) {
                            NightmareEntity nightmare = ModEntities.NIGHTMARE.create(serverWorld);
                            if (nightmare != null) {
                                nightmare.refreshPositionAndAngles(cavePos.getX() + 0.5, cavePos.getY(), cavePos.getZ() + 0.5, serverWorld.random.nextFloat() * 360F, 0.0F);
                                serverWorld.spawnEntity(nightmare);
                            }
                        }
                    }
                }
            }

            if (isWorldCursed) {
                ServerWorld overworld = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (overworld != null && !overworld.isRaining()) {
                    overworld.setWeather(0, 1000000, true, true);
                }
            }
        });

        // Регистрация всего остального
        ModItemGroups.registerItemGroups();
        ModSounds.register();
        ModBlocks.registerModBlocks();
        ModEntities.registerModEntities();
        ModWorldGen.generateModWorldGen();
        com.crimsonmoon.world.WatcherManager.register();
        ThorncallerSpawnManager.register();
        NoDropRegistry.protect(ModItems.BLOOD_MONOLIT);
        
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

    // --- ЛОГИКА УМНОГО ПОИСКА МЕСТА ДЛЯ СТРУКТУРЫ ---
    private static void spawnMonolitNearPlayer(ServerWorld world, BlockPos playerPos) {
        BlockPos spawnPos = null;
        
        // Делаем до 150 попыток
        for (int i = 0; i < 150; i++) {
            // Ищем точку в радиусе от 15 до 40 блоков от игрока
            int dx = (world.random.nextInt(26) + 15) * (world.random.nextBoolean() ? 1 : -1);
            int dz = (world.random.nextInt(26) + 15) * (world.random.nextBoolean() ? 1 : -1);
            
            int x = playerPos.getX() + dx;
            int z = playerPos.getZ() + dz;
            
            // Находим самый верхний блок
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState stateBelow = world.getBlockState(checkPos.down());
            
            // 1. Проверяем, чтобы фундамент был природным
            boolean isNaturalGround = stateBelow.isOf(Blocks.GRASS_BLOCK) || 
                                      stateBelow.isOf(Blocks.DIRT) || 
                                      stateBelow.isOf(Blocks.STONE) || 
                                      stateBelow.isOf(Blocks.SAND) || 
                                      stateBelow.isOf(Blocks.PODZOL) || 
                                      stateBelow.isOf(Blocks.SNOW_BLOCK);

            if (isNaturalGround) {
                // 2. Сканируем пространство над землей
                boolean isClear = true;
                
                for (int checkX = -2; checkX <= 2; checkX++) {
                    for (int checkY = 0; checkY <= 5; checkY++) {
                        for (int checkZ = -2; checkZ <= 2; checkZ++) {
                            BlockPos airCheckPos = checkPos.add(checkX, checkY, checkZ);
                            BlockState airState = world.getBlockState(airCheckPos);
                            
                            if (!airState.isAir() && !airState.isReplaceable()) {
                                isClear = false;
                                break;
                            }
                        }
                        if (!isClear) break;
                    }
                    if (!isClear) break;
                }
                
                if (isClear) {
                    spawnPos = checkPos;
                    break;
                }
            }
        }

        // Если нашли поляну, ставим структуру
        if (spawnPos != null) {
            Identifier structureId = new Identifier(MOD_ID, "crimsonmoon");
            
            // ВОТ ТУТ ИСПРАВЛЕНИЕ: Опускаем позицию установки на 1 блок вниз, чтобы врасти в землю
            BlockPos finalSpawnPos = spawnPos.down(1);
            
            world.getStructureTemplateManager().getTemplate(structureId).ifPresent(template -> {
                StructurePlacementData placementData = new StructurePlacementData()
                    .setRotation(BlockRotation.NONE)
                    .setMirror(BlockMirror.NONE)
                    .setIgnoreEntities(false);
                
                // Ставим NBT-структуру в мир
                template.place(world, finalSpawnPos, finalSpawnPos, placementData, world.random, 2);
            });
        }
    }

    // Вспомогательный метод: поиск просторной пещеры под землей для босса
    private static BlockPos findSpaciousCave(ServerWorld world, BlockPos center, int radius) {
        for (int i = 0; i < 40; i++) {
            BlockPos checkPos = center.add(
                world.random.nextInt(radius * 2) - radius,
                world.random.nextInt(30) - 15,
                world.random.nextInt(radius * 2) - radius
            );

            if (checkPos.getY() < 50 && world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                boolean hasSpace = true;
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (!world.getBlockState(checkPos.add(x, y, z)).isAir()) {
                                hasSpace = false;
                                break;
                            }
                        }
                    }
                }
                if (hasSpace) return checkPos;
            }
        }
        return null;
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}