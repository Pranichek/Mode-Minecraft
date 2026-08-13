package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.NightmareEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NightmareEntityRenderer extends GeoEntityRenderer<NightmareEntity> {

    public NightmareEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NightmareModel());
        // радиус тени под ногами; можно поменять под финальную модель
        this.shadowRadius = 0.7f;
    }
}
