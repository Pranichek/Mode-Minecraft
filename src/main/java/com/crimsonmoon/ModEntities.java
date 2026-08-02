package com.crimsonmoon;

import com.crimsonmoon.entity.BloodwalkerEntity;
import com.crimsonmoon.entity.WatcherEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<BloodwalkerEntity> BLOODWALKER = Registry.register(
        Registries.ENTITY_TYPE,
        CrimsonMoon.id("bloodwalker"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, BloodwalkerEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f)) // как у зомби
            .build()
    );

    public static final EntityType<WatcherEntity> WATCHER = Registry.register(
    Registries.ENTITY_TYPE,
    CrimsonMoon.id("watcher"),
    FabricEntityTypeBuilder.create(SpawnGroup.MISC, WatcherEntity::new) // MISC — не участвует в обычном мобкапе/спавн-циклах
        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
        .build()
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(BLOODWALKER, BloodwalkerEntity.createBloodwalkerAttributes());
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createWatcherAttributes()); 
    }
}