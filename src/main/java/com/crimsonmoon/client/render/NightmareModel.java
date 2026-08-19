package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.entity.NightmareEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Указывает GeckoLib, где искать модель/анимации/текстуру Nightmare.
 *
 * Чтобы заменить модель или добавить анимации — просто перезапиши
 * соответствующий файл, ничего в этом классе менять не нужно:
 *   assets/crimsonmoon/geo/nightmare.geo.json          — модель (экспорт из Blockbench: "Bedrock Geometry" / GeckoLib)
 *   assets/crimsonmoon/animations/nightmare.animation.json — анимации (экспорт анимаций из Blockbench)
 *   assets/crimsonmoon/textures/entity/nightmare/nightmare.png — текстура
 */
public class NightmareModel extends GeoModel<NightmareEntity> {

    @Override
    public Identifier getModelResource(NightmareEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "geo/nightmare.geo.json");
    }

    @Override
    public Identifier getTextureResource(NightmareEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/nightmare/nightmare.png");
    }

    @Override
    public Identifier getAnimationResource(NightmareEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "animations/nightmare.animation.json");
    }
}