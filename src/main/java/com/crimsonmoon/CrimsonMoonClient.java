package com.crimsonmoon;

import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
    }
}