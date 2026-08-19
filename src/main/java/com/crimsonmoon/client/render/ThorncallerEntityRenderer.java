package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.ThorncallerEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Текстура/модель/анимации грузятся через ThorncallerModel:
// src/main/resources/assets/crimsonmoon/geo/thorncaller.geo.json
// src/main/resources/assets/crimsonmoon/animations/thorncaller.animation.json
// src/main/resources/assets/crimsonmoon/textures/entity/thorncaller/thorncaller.png (128x128)

public class ThorncallerEntityRenderer extends GeoEntityRenderer<ThorncallerEntity> {

    public ThorncallerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ThorncallerModel());
        this.shadowRadius = 0.6f;
    }
}