package com.crimsonmoon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;
import com.crimsonmoon.client.render.NightmareEntityRenderer;
import com.crimsonmoon.client.render.ThorncallerEntityRenderer;
import com.crimsonmoon.mixin.GameRendererInvoker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;

import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.Identifier;

public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);

        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearSoundSystem.register();

        TrinketRendererRegistry.registerRenderer(ModItems.BLOOD_NECKLACE,
            (stack, slotReference, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {
                if (entity instanceof AbstractClientPlayerEntity player && contextModel instanceof PlayerEntityModel) {
                    @SuppressWarnings("unchecked")
                    PlayerEntityModel<AbstractClientPlayerEntity> playerModel = (PlayerEntityModel<AbstractClientPlayerEntity>) contextModel;

                    matrices.push();

                    // ставим ожерелье на грудь и следуем за поворотом тела
                    TrinketRenderer.translateToChest(matrices, playerModel, player);

                    // позиция: Y поднимает к шее, Z прижимает к груди
                    matrices.translate(0.0F, 0.28F, -0.16F);
                    matrices.scale(0.5F, 0.5F, 0.5F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));

                    MinecraftClient.getInstance().getItemRenderer().renderItem(
                        stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0
                    );

                    matrices.pop();
                }
            }
        );

        // --- ЛОГИКА СЕРОГО ЭКРАНА ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                boolean isCursed = TrinketsApi.getTrinketComponent(client.player)
                    .map(comp -> comp.isEquipped(ModItems.BLOOD_NECKLACE))
                    .orElse(false);

                if (isCursed) {
                    if (client.gameRenderer.getPostProcessor() == null) {
                        ((GameRendererInvoker) client.gameRenderer).invokeLoadPostProcessor(new Identifier("minecraft", "shaders/post/desaturate.json"));
                    }
                } else {
                    if (client.gameRenderer.getPostProcessor() != null) {
                        client.gameRenderer.disablePostProcessor();
                    }
                }
            }
        });
    }
}