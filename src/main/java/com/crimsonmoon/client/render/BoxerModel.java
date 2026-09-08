package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.entity.BoxerEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BoxerModel extends GeoModel<BoxerEntity> {

    @Override
    public Identifier getModelResource(BoxerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "geo/boxer.geo.json");
    }

    @Override
    public Identifier getTextureResource(BoxerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/boxer/boxer.png");
    }

    @Override
    public Identifier getAnimationResource(BoxerEntity entity) {
        return Identifier.of(CrimsonMoon.MOD_ID, "animations/boxer.animation.json");
    }
}