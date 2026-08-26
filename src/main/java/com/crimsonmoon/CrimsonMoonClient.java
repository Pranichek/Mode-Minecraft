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
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;

import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import dev.emi.trinkets.api.TrinketsApi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.Identifier;

public class CrimsonMoonClient implements ClientModInitializer {

    // идентификатор нашей отдельной 3D-модели для тела
    private static final ModelIdentifier NECKLACE_WORN_MODEL =
        new ModelIdentifier("crimsonmoon", "blood_necklace_worn", "inventory");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);

        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearSoundSystem.register();

        // Регистрируем нашу 3D-модель, чтобы игра её загрузила и запекла
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(NECKLACE_WORN_MODEL);
        });

        TrinketRendererRegistry.registerRenderer(ModItems.BLOOD_NECKLACE,
            (stack, slotReference, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {
                if (entity instanceof AbstractClientPlayerEntity player && contextModel instanceof PlayerEntityModel) {
                    @SuppressWarnings("unchecked")
                    PlayerEntityModel<AbstractClientPlayerEntity> playerModel = (PlayerEntityModel<AbstractClientPlayerEntity>) contextModel;

                    MinecraftClient mc = MinecraftClient.getInstance();

                    // берём именно 3D-модель, а не иконку предмета
                    BakedModel wornModel = mc.getBakedModelManager().getModel(NECKLACE_WORN_MODEL);

                    matrices.push();

                    // привязываем к груди и следуем за поворотом тела
                    TrinketRenderer.translateToChest(matrices, playerModel, player);

                    // сначала разворачиваем, потом позиционируем (так оси интуитивны)
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));

                    // нормальный размер и позиция на груди/шее
                    // X — влево/вправо, Y — вверх(к шее)/вниз, Z — вперёд/прижать к телу
                    matrices.scale(0.45F, 0.45F, 0.45F);
                    matrices.translate(-0.5F, 0.2F, -0.15F);

                    VertexConsumer vc = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutout(
                            new Identifier("crimsonmoon", "textures/item/blood_necklace_texture.png")
                        )
                    );

                    mc.getBlockRenderManager().getModelRenderer().render(
                        matrices.peek(),
                        vc,
                        null,
                        wornModel,
                        1.0F, 1.0F, 1.0F,
                        light,
                        OverlayTexture.DEFAULT_UV
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