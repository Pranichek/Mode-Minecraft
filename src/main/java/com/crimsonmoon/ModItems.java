package com.crimsonmoon;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;


public class ModItems {
    public static final Item BLOOD_STONE = registerItem(
        "blood_stone",
        new Item(new Item.Settings())
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
            });
    }
}
