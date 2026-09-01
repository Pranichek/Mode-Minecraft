package com.crimsonmoon;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

/**
 * Ключ измерения боссарены. Само измерение целиком описано данными
 * (датапаком) мода:
 *  - тип измерения: data/crimsonmoon/dimension_type/boss_realm.json
 *  - само измерение (генератор): data/crimsonmoon/dimension/boss_realm.json
 *  - структура арены: data/crimsonmoon/structure/boss_arena.nbt
 * Регистрировать это в коде отдельно не нужно — Fabric API подхватывает
 * датапак-измерения мода автоматически (в т.ч. в уже существующих мирах,
 * не только в новых) благодаря модулю fabric-dimensions-v1.
 */
public final class ModDimensions {

    public static final RegistryKey<World> BOSS_REALM =
        RegistryKey.of(RegistryKeys.WORLD, CrimsonMoon.id("boss_realm"));

    private ModDimensions() {}
}
