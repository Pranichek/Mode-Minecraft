package com.crimsonmoon;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup CRIMSON_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        new Identifier(CrimsonMoon.MOD_ID, "crimson_group"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.crimson_group"))
            .icon(() -> new ItemStack(ModItems.BLOOD_STONE)) // Іконка вкладки
            .entries((displayContext, entries) -> {
                // Додаємо блоки
                entries.add(ModBlocks.CRIMSON_ORE);
                
                // Додаємо ресурси
                entries.add(ModItems.BLOOD_STONE);
                entries.add(ModItems.BLOOD_INGOT);
                
                // Додаємо броню
                entries.add(ModItems.BLOOD_HELMET);
                entries.add(ModItems.BLOOD_CHESTPLATE);
                entries.add(ModItems.BLOOD_LEGGINGS);
                entries.add(ModItems.BLOOD_BOOTS);
            })
            .build()
    );

    public static void registerItemGroups() {
        
    }
}