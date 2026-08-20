package com.crimsonmoon;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {
    public static final Block CRIMSON_ORE = registerBlock(
        "crimson_ore",
        new Block(FabricBlockSettings.copyOf(Blocks.GOLD_ORE)
            .strength(4.0f, 4.0f)
            .requiresTool()
        )
    );

    public static final Block MONOLIT = registerBlock(
        "monolit",
        new Block(FabricBlockSettings.copyOf(Blocks.DIAMOND_ORE)
        .strength(3.0f, 3.0f)
        .requiresTool()
            )
    );

    private static Block registerBlock(String name, Block block) {
        Registry.register(
            Registries.BLOCK,
            CrimsonMoon.id(name),
            block
        );
        registerBlockItem(name, block);
        return block;
    }


    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(
            Registries.ITEM,
            CrimsonMoon.id(name),
            new BlockItem(block, new Item.Settings())
        );
    }

    public static void registerModBlocks() {

    }
}
