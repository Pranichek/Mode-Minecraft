package com.crimsonmoon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;
import com.crimsonmoon.client.render.NightmareEntityRenderer;
import com.crimsonmoon.client.render.ThorncallerEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;


public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);

        // HudRenderCallback.EVENT.register(new ParanoiaOverlay());
        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearSoundSystem.register();
    }
}