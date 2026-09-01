package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.MonsterikEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Текстура/модель/анимации грузятся через MonsterikModel:
// src/main/resources/assets/crimsonmoon/geo/monsterik.geo.json
// src/main/resources/assets/crimsonmoon/animations/monsterik.animation.json
// src/main/resources/assets/crimsonmoon/textures/entity/monsterik/monsterik.png (128x128)

public class MonsterikEntityRenderer extends GeoEntityRenderer<MonsterikEntity> {

    public MonsterikEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MonsterikModel());
        this.shadowRadius = 0.9f;
    }
}
