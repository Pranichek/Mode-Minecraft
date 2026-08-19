package com.crimsonmoon.potion;

import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModPotions {
    
    // Создаем само зелье (Регенерация 2-го уровня на 20 секунд / 400 тиков)
    public static final Potion CRIMSON_REGEN = registerPotion("crimson_regen", 
            new Potion(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1)));

    // Метод, который регистрирует зелья и их рецепты
    public static void registerPotions() {
        FabricBrewingRecipeRegistry.registerPotionRecipe(
            Potions.AWKWARD, 
            Ingredient.ofItems(Items.CRIMSON_FUNGUS), 
            CRIMSON_REGEN
        );
    }

    // Вспомогательный метод для регистрации в движке игры
    private static Potion registerPotion(String name, Potion potion) {
        return Registry.register(Registries.POTION, new Identifier("crimsonmoon", name), potion);
    }
}