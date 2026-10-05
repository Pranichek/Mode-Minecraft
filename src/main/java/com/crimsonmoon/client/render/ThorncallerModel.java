package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.entity.ThorncallerEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Указывает GeckoLib, где искать модель/анимации/текстуру Thorncaller.
 *
 *   assets/crimsonmoon/geo/thorncaller.geo.json                — модель (Blockbench: "Bedrock Geometry" / GeckoLib)
 *   assets/crimsonmoon/animations/thorncaller.animation.json   — анимации (экспорт анимаций из Blockbench)
 *   assets/crimsonmoon/textures/entity/thorncaller/thorncaller.png — текстура
 */
public class ThorncallerModel extends GeoModel<ThorncallerEntity> {

    @Override
    public Identifier getModelResource(ThorncallerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "geo/thorncaller.geo.json");
    }

    @Override
    public Identifier getTextureResource(ThorncallerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/thorncaller/thorncaller.png");
    }

    @Override
    public Identifier getAnimationResource(ThorncallerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "animations/thorncaller.animation.json");
    }
}