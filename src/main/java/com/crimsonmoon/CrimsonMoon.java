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

public class CrimsonMoon implements ModInitializer {
	public static final String MOD_ID = "crimsonmoon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();
		ModWorldGen.generateModWorldGen();
		ModSounds.registerModSounds();
		com.crimsonmoon.world.WatcherManager.register();
		ThorncallerSpawnManager.register();

		NoDropRegistry.protect(ModItems.BLOOD_INGOT);

		ThorncallerEntity.MINION_TYPE = ModEntities.BLOODWALKER;

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			for (ItemStack stack : PendingItemStash.take(oldPlayer.getUuid())) {
				if (!newPlayer.getInventory().insertStack(stack)) {
					newPlayer.dropItem(stack, false);
				}
			}
		});
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
