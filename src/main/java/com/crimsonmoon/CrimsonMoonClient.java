package com.crimsonmoon;

import com.crimsonmoon.client.gui.ParanoiaOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;


public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);

        // HudRenderCallback.EVENT.register(new ParanoiaOverlay());
        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearSoundSystem.register();
    }
}