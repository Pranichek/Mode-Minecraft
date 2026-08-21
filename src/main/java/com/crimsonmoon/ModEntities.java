package com.crimsonmoon;

import com.crimsonmoon.entity.BloodwalkerEntity;
import com.crimsonmoon.entity.NightmareEntity;
import com.crimsonmoon.entity.WatcherEntity;
import com.crimsonmoon.entity.ThorncallerEntity;
import com.crimsonmoon.entity.BloodBoltEntity;

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

    public static final EntityType<NightmareEntity> NIGHTMARE = Registry.register(
        Registries.ENTITY_TYPE,
        CrimsonMoon.id("nightmare"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, NightmareEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 3.6f)) // высота 3 блока, как у эндермена
            .build()
    );

    public static final EntityType<ThorncallerEntity> THORNCALLER = Registry.register(
        Registries.ENTITY_TYPE,
        CrimsonMoon.id("thorncaller"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ThorncallerEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
            .build()
    );

    public static final EntityType<BloodBoltEntity> BLOOD_BOLT = Registry.register(
        Registries.ENTITY_TYPE,
        CrimsonMoon.id("blood_bolt"),
        FabricEntityTypeBuilder.<BloodBoltEntity>create(SpawnGroup.MISC, BloodBoltEntity::new)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(10)
            .build()
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(BLOODWALKER, BloodwalkerEntity.createBloodwalkerAttributes());
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createWatcherAttributes());
        FabricDefaultAttributeRegistry.register(NIGHTMARE, NightmareEntity.createNightmareAttributes());
        FabricDefaultAttributeRegistry.register(THORNCALLER, ThorncallerEntity.createThorncallerAttributes());
    }
}