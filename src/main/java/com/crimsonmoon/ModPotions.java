package com.crimsonmoon;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModPotions {

    // Зілля битви: Сила II, Регенерація II, 5 хв
    public static final Potion BATTLE_POTION = registerPotion(
        "battle_potion",
        new Potion(
            "battle_potion",
            new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1),
            new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1)
        )
    );

    // Зілля берсерка: Сила III, Швидкість II, Захист II, 4 хв
    public static final Potion BERSERKER_POTION = registerPotion(
        "berserker_potion",
        new Potion(
            "berserker_potion",
            new StatusEffectInstance(StatusEffects.STRENGTH, 4800, 2),
            new StatusEffectInstance(StatusEffects.SPEED, 4800, 1),
            new StatusEffectInstance(StatusEffects.RESISTANCE, 4800, 1)
        )
    );

    // Геройське зілля: Сила IV, Регенерація III, Захист III, Швидкість III,
    // Стрибучість II, Нічний зір, Підводне дихання II, 10 хв
    public static final Potion HEROIC_POTION = registerPotion(
        "heroic_potion",
        new Potion(
            "heroic_potion",
            new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 3),
            new StatusEffectInstance(StatusEffects.REGENERATION, 12000, 2),
            new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 2),
            new StatusEffectInstance(StatusEffects.SPEED, 12000, 2),
            new StatusEffectInstance(StatusEffects.JUMP_BOOST, 12000, 1),
            new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0),
            new StatusEffectInstance(StatusEffects.WATER_BREATHING, 12000, 1)
        )
    );

    private static Potion registerPotion(String id, Potion potion) {
        return Registry.register(Registries.POTION, CrimsonMoon.id(id), potion);
    }

    public static void register() {
    }
}