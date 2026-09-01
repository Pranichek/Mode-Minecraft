package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.entity.MonsterikEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Указывает GeckoLib, где искать модель/анимации/текстуру босса Monsterik.
 *
 *   assets/crimsonmoon/geo/monsterik.geo.json               — модель (Blockbench)
 *   assets/crimsonmoon/animations/monsterik.animation.json  — анимации (walk/run/attack)
 *   assets/crimsonmoon/textures/entity/monsterik/monsterik.png — текстура (скин)
 */
public class MonsterikModel extends GeoModel<MonsterikEntity> {

    @Override
    public Identifier getModelResource(MonsterikEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "geo/monsterik.geo.json");
    }

    @Override
    public Identifier getTextureResource(MonsterikEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/monsterik/monsterik.png");
    }

    @Override
    public Identifier getAnimationResource(MonsterikEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "animations/monsterik.animation.json");
    }
}
