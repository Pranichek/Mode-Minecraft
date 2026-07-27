package com.crimsonmoon.world;

import com.crimsonmoon.CrimsonMoon;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModWorldGen {

    private static final RegistryKey<PlacedFeature> CRIMSON_ORE_PLACED =
            RegistryKey.of(
                    RegistryKeys.PLACED_FEATURE,
                    CrimsonMoon.id("crimson_ore")
            );
    public static void generateModWorldGen() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                CRIMSON_ORE_PLACED
        );
    }
}