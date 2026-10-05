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
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.World;
import java.util.List;

import com.crimsonmoon.item.BloodNecklaceItem;
import com.crimsonmoon.item.CrimsonShieldItem;
import com.crimsonmoon.item.PortalKeyItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import com.crimsonmoon.item.BloodStaffItem;
import com.crimsonmoon.item.BloodMaceItem;
import com.crimsonmoon.item.BloodBowItem;
import com.crimsonmoon.item.BloodClotItem;

public class ModItems {

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

    public static final Item BLOOD_MACE = registerItem(
        "blood_mace",
        new BloodMaceItem(new Item.Settings())
    );

    public static final Item BLOOD_BOW = registerItem(
        "blood_bow",
        new BloodBowItem(new Item.Settings().maxCount(1).maxDamage(ModToolMaterials.BLOOD.getDurability()))
    );

    public static final Item BLOOD_CLOT = registerItem(
        "blood_clot",
        new BloodClotItem(new Item.Settings())
    );

    public static final Item BLOOD_STONE = registerItem(
        "blood_stone",
        new Item(new Item.Settings())
    );

    public static final Item BLOOD_INGOT = registerItem(
        "blood_ingot",
        new Item(new Item.Settings())
    );

    public static final Item BLOOD_MONOLIT = registerItem(
        "blood_monolit",
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
    
    public static final Item BLOOD_SHIELD = registerItem(
        "crimson_shield_item",
        new CrimsonShieldItem(new FabricItemSettings().maxDamage(500))
    );

    public static final Item BLOOD_NECKLACE = registerItem("blood_necklace", 
        new BloodNecklaceItem(new FabricItemSettings().maxCount(1)));

    public static final Item XRAY_GLASSES = registerItem("xray_glasses", new Item(new FabricItemSettings().maxCount(1)) {
        
        @Override
        public Text getName(ItemStack stack) {
            return Text.literal("Окуляри Астральної Проєкції").formatted(Formatting.AQUA, Formatting.BOLD);
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            tooltip.add(Text.literal("Реліквія переможеного кошмару").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
            tooltip.add(Text.literal(" "));
            tooltip.add(Text.literal("Як використовувати:").formatted(Formatting.GRAY));
            tooltip.add(Text.literal("1. Покладіть у слот для капелюха/обличчя (Trinkets)").formatted(Formatting.GRAY));
            tooltip.add(Text.literal("2. Натисніть клавішу [R]").formatted(Formatting.GRAY));
            tooltip.add(Text.literal(" "));
            tooltip.add(Text.literal("Дає можливість залишити тіло").formatted(Formatting.GOLD));
            tooltip.add(Text.literal("та літати крізь стіни на 10 секунд!").formatted(Formatting.GOLD));
        }
    });

    private static Item registerItem(String name, Item item){
        return Registry.register(
            Registries.ITEM,
            CrimsonMoon.id(name),
            item
        );
    }

   public static final Item PORTAL_KEY = registerItem("portal_key", new PortalKeyItem(new FabricItemSettings()));
}