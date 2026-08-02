package com.crimsonmoon;

import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Быстрый старт: переиспользуем модель и анимации зомби,
        // просто со своей текстурой (см. ниже)
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
    }
}