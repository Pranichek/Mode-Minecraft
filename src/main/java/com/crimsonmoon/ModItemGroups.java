package com.crimsonmoon; 

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup; 
import net.minecraft.item.ItemGroup; 
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack; 
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries; 
import net.minecraft.registry.Registry; 
import net.minecraft.text.Text; 
import net.minecraft.util.Identifier;

public class ModItemGroups{
    public static final ItemGroup CRIMSON_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        new Identifier(CrimsonMoon.MOD_ID, "crimson_group"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.crimson_group"))
            .icon(() -> new ItemStack(ModItems.BLOOD_STONE))
            .entries((displayContext, entries) -> {

                entries.add(ModBlocks.CRIMSON_ORE);
                entries.add(ModBlocks.MONOLIT);
                
                entries.add(ModItems.BLOOD_STONE);
                entries.add(ModItems.BLOOD_INGOT);
                entries.add(ModItems.BLOOD_MONOLIT);
                
                entries.add(ModItems.BLOOD_HELMET);
                entries.add(ModItems.BLOOD_CHESTPLATE);
                entries.add(ModItems.BLOOD_LEGGINGS);
                entries.add(ModItems.BLOOD_BOOTS);
                entries.add(ModItems.BLOOD_NECKLACE);

                entries.add(ModItems.BLOOD_SWORD);
                entries.add(ModItems.BLOOD_PICKAXE);
                entries.add(ModItems.BLOOD_AXE);
                entries.add(ModItems.BLOOD_SHOVEL);
                entries.add(ModItems.BLOOD_HOE);
                entries.add(ModItems.BLOOD_SHIELD);
                entries.add(ModItems.NIGHTMARE_SPAWN_EGG);
                entries.add(ModItems.THORNCALLER_SPAWN_EGG);
                entries.add(ModItems.BLOODWALKER_SPAWN_EGG);
                entries.add(ModItems.BLOOD_STAFF);
                entries.add(ModItems.BLOOD_MACE);
                entries.add(ModItems.BLOOD_BOW);
                entries.add(ModItems.BLOOD_CLOT);

                entries.add(PotionUtil.setPotion(new ItemStack(Items.POTION), ModPotions.BATTLE_POTION));
                entries.add(PotionUtil.setPotion(new ItemStack(Items.POTION), ModPotions.BERSERKER_POTION));
                entries.add(PotionUtil.setPotion(new ItemStack(Items.POTION), ModPotions.HEROIC_POTION));
                
            })
            .build()
    );

    public static void registerItemGroups(){
        // метод для ініціалізації
    }
}