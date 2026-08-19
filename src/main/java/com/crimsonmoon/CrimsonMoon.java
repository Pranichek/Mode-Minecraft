package com.crimsonmoon;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crimsonmoon.world.ModWorldGen;

import net.fabricmc.api.ModInitializer; 
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

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static boolean STATE_CURSED = false;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// відслідковування ломання блоку
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			
			if(!world.isClient() && state.isOf(Blocks.DIAMOND_BLOCK) && !STATE_CURSED) {
				if (!player.getCommandTags().contains("cursed_player")){
					player.addCommandTag("cursed_player");
					
					ServerWorld serverWorld = (ServerWorld) world;
					serverWorld.setWeather(0, 6000, true, true);
					
					player.sendMessage(Text.literal("Прокляття спостерігача пробудилося!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
					
					world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.0f, 0.8f);
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0, false, false, true));
					STATE_CURSED = true;
				}

			}
			
		});

		// ефекти при отриманні прокляття
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.getCommandTags().contains("cursed_player")) {
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
							serverWorld.random.nextInt(radius * 2 )- radius,
							serverWorld.random.nextInt(radius * 2 )- radius,
							serverWorld.random.nextInt(radius * 2 )- radius
						);
						
						BlockState targetState = serverWorld.getBlockState(targetPos);
						
						if(targetState.isOf(Blocks.TORCH) || targetState.isOf(Blocks.WALL_TORCH)){
							serverWorld.breakBlock(targetPos, false);
							serverWorld.playSound(null, targetPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f);
							serverWorld.spawnParticles(
								ParticleTypes.SMOKE,
								targetPos.getX() + 0.5,
								targetPos.getY() + 0.5,
								targetPos.getZ() + 0.5,
								10, 0.1, 0.1, 0.1, 0.02
							);
							
						}
						

						
					}
				}
			}
		});

		
		// Реєстрація пакетів 
		ModItemGroups.registerItemGroups();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();
		ModWorldGen.generateModWorldGen();

		ModSounds.registerSounds();
		ParanoiaSystem.register();

		ModPotions.registerPotions();
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
