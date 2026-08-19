package com.crimsonmoon;

import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorItem.Type;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum ModArmorMaterials implements ArmorMaterial {
    CRIMSON("crimson");

    private final String name;

    ModArmorMaterials(String name) {
        this.name = name;
    }

    @Override
    public int getDurability(Type type) {
        return 195;
    }

    @Override
    public int getEnchantability() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_IRON;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1f;
    }

    @Override
    public String getName() {
        return CrimsonMoon.MOD_ID + ":" + this.name;
    }

    @Override
    public int getProtection(Type type) {
        if (type == Type.HELMET) {
            return 3;
        } else if (type == Type.CHESTPLATE) {
            return 8;
        } else if (type == Type.LEGGINGS) {
            return 6;
        } else if (type == Type.BOOTS) {
            return 3;
        }
        return 0;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.BLOOD_INGOT);
    }

    @Override
    public float getToughness() {
       return 1.0f;
    }
}