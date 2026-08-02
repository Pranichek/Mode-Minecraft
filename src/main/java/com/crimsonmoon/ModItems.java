package com.crimsonmoon;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.item.ArmorItem;


public class ModItems {
    public static final Item BLOOD_STONE = registerItem(
        "blood_stone",
        new Item(new Item.Settings())
    );

    public static final Item BLOOD_INGOT = registerItem(
        "blood_ingot",
        new Item(new Item.Settings())
    );
 
    public static final Item BLOOD_HELMET = registerItem(
        "blood_helmet",
        new CrimsonArmorItem(ModArmorMaterials.CRIMSON, 
        ArmorItem.Type.HELMET, 
        new Item.Settings())
    );

    public static final Item BLOOD_CHESTPLATE = registerItem(
        "blood_chestplate",
        new CrimsonArmorItem(ModArmorMaterials.CRIMSON, 
        ArmorItem.Type.CHESTPLATE, 
        new Item.Settings())
    );

    public static final Item BLOOD_LEGGINGS = registerItem(
        "blood_leggings",
        new CrimsonArmorItem(ModArmorMaterials.CRIMSON, 
        ArmorItem.Type.LEGGINGS, 
        new Item.Settings())
    );

    public static final Item BLOOD_BOOTS = registerItem(
        "blood_boots",
        new CrimsonArmorItem(ModArmorMaterials.CRIMSON, 
        ArmorItem.Type.BOOTS, 
        new Item.Settings())
    );

    private static Item registerItem(String name, Item item){
        return Registry.register(
            Registries.ITEM,
            CrimsonMoon.id(name),
            item
        );
    }
    
    public static void registerModItems(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
            .register(entries -> {
                entries.add(BLOOD_STONE);
                entries.add(BLOOD_INGOT);
            });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
            .register(entries -> {
                entries.add(BLOOD_HELMET);
                entries.add(BLOOD_CHESTPLATE);
                entries.add(BLOOD_LEGGINGS);
                entries.add(BLOOD_BOOTS);
            });
    }
}