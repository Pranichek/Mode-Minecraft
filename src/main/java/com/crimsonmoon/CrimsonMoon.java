package com.crimsonmoon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crimsonmoon.item.NoDropRegistry;
import com.crimsonmoon.item.PendingItemStash;
import com.crimsonmoon.world.ModWorldGen;
import com.crimsonmoon.world.ThorncallerSpawnManager;
import com.crimsonmoon.entity.ThorncallerEntity; 
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
import com.crimsonmoon.potion.ModPotions;

public class CrimsonMoon implements ModInitializer {
    public static final String MOD_ID = "crimsonmoon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean STATE_CURSED = false;

    @Override
    public void onInitialize() {
        // отслеживание разрушения блока
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if(!world.isClient() && state.isOf(ModBlocks.MONOLIT) && !STATE_CURSED) {
                if (!player.getCommandTags().contains("cursed_player")){
                    player.addCommandTag("cursed_player");
                    
                    // --- ВОТ ЭТОТ НОВЫЙ КОД НАДЕВАЕТ ОЖЕРЕЛЬЕ ---
                    TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
                        trinkets.getInventory().get("chest").get("necklace").setStack(0, new ItemStack(ModItems.BLOOD_NECKLACE));
                    });
                    
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.setWeather(0, 6000, true, true);
                    
                    player.sendMessage(Text.literal("Прокляття спостерігача пробудилося!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                    
                    world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.0f, 0.8f);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0, false, false, true));
                    STATE_CURSED = true;
                }
            }
        });

        // эффекты при получении проклятия
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

            // ЛОГИКА ПОГОДЫ: если мир проклят, вызываем вечную грозу
            if (isWorldCursed) {
                ServerWorld overworld = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (overworld != null && !overworld.isRaining()) {
                    overworld.setWeather(0, 1000000, true, true);
                }
            }
        });

        // Реєстрація пакетів 
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
        ModPotions.registerPotions();
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}