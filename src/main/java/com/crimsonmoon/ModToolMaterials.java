package com.crimsonmoon;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public enum ModToolMaterials implements ToolMaterial{
    BLOOD(3, 1800, 8.5f, 3.5f, 15);

    private final int minigLevel; // рівень копання
    private final int itemDurability; // міцність
    private final float miningSpeed; // швидкість копання
    private final float attackDamage; // базова шкода
    private final int enchantability; // шанс зачаровування

    ModToolMaterials(int miningLevel, int ItemDurability, float miningSpeed, float attackDamage, int enchantability){
        this.minigLevel = miningLevel;
        this.itemDurability = ItemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
    }

    @Override
    public int getDurability(){
        return this.itemDurability;
    }

    @Override
    public float getMiningSpeedMultiplier(){
        return this.miningSpeed;
    }

    @Override
    public float getAttackDamage(){
        return this.attackDamage;
    }

    @Override
    public int getMiningLevel(){
        return this.minigLevel;
    }

    @Override
    public int getEnchantability(){
        return this.enchantability;
    }

    @Override
    public Ingredient getRepairIngredient(){
        return Ingredient.ofItems(ModItems.BLOOD_INGOT);
    }
}   
