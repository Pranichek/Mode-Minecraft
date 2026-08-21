package com.crimsonmoon;

import net.minecraft.item.SwordItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.item.ArmorItem;
import com.crimsonmoon.item.CrimsonShieldItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

import com.crimsonmoon.item.BloodStaffItem;



public class ModItems {
    // яйця

    public static final Item THORNCALLER_SPAWN_EGG = registerItem(
        "thorncaller_spawn_egg",
        new SpawnEggItem(ModEntities.THORNCALLER, 0x245C32, 0x6B2D7C, new Item.Settings())
    );

    public static final Item BLOODWALKER_SPAWN_EGG = registerItem(
        "bloodwalker_spawn_egg",
        new SpawnEggItem(ModEntities.BLOODWALKER, 0x940000, 0x000000, new Item.Settings())
    );

    public static final Item NIGHTMARE_SPAWN_EGG = registerItem(
        "nightmare_spawn_egg",
        new SpawnEggItem(ModEntities.NIGHTMARE, 0x1a1a1a, 0x8b0000, new Item.Settings())
    );
    
    // інструменти
    public static final Item BLOOD_SWORD = registerItem(
        "blood_sword",
        new SwordItem(ModToolMaterials.BLOOD, 3, -2.4f, new Item.Settings())
    );

    public static final Item BLOOD_PICKAXE = registerItem(
        "blood_pickaxe",
        new PickaxeItem(ModToolMaterials.BLOOD, 1, -2.8f, new Item.Settings())
    );

    public static final Item BLOOD_AXE = registerItem(
        "blood_axe",
        new AxeItem(ModToolMaterials.BLOOD, 5.0f, -3.0f, new Item.Settings())
    );

    public static final Item BLOOD_SHOVEL = registerItem(
        "blood_shovel",
        new ShovelItem(ModToolMaterials.BLOOD, 1.5f, -3.0f, new Item.Settings())
    );

    public static final Item BLOOD_HOE = registerItem(
        "blood_hoe",
        new HoeItem(ModToolMaterials.BLOOD, 3, 0.0f, new Item.Settings())
    );

    public static final Item BLOOD_STAFF = registerItem(
        "blood_staf",
        new BloodStaffItem(new Item.Settings().maxCount(1))
    );

    // руди
    public static final Item BLOOD_STONE = registerItem(
        "blood_stone",
        new Item(new Item.Settings())
    );

    public static final Item BLOOD_INGOT = registerItem(
        "blood_ingot",
        new Item(new Item.Settings())
    );

    // броня
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
    
    public static final Item BLOOD_SHIELD = registerItem(
        "crimson_shield_item",
        new CrimsonShieldItem(new FabricItemSettings().maxDamage(500))
    );

    private static Item registerItem(String name, Item item){
        return Registry.register(
            Registries.ITEM,
            CrimsonMoon.id(name),
            item
        );
    }
}