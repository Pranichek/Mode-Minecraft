package com.crimsonmoon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;
import com.crimsonmoon.client.render.NightmareEntityRenderer;
import com.crimsonmoon.client.render.ThorncallerEntityRenderer;
import com.crimsonmoon.client.render.BloodBoltEntityRenderer;
import com.crimsonmoon.client.render.BloodClotEntityRenderer;
import com.crimsonmoon.mixin.GameRendererInvoker;
import com.crimsonmoon.client.render.MonsterikEntityRenderer;
import com.crimsonmoon.client.render.BoxerEntityRenderer;

import com.crimsonmoon.client.gui.BossJournalScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;

import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;

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

public class CrimsonMoonClient implements ClientModInitializer {

    private static final ModelIdentifier NECKLACE_WORN_MODEL =
        new ModelIdentifier("crimsonmoon", "blood_necklace_worn", "inventory");

    private static KeyBinding journalKey;
    private static KeyBinding xrayKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_BOLT, BloodBoltEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_CLOT, BloodClotEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MONSTERIK, MonsterikEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOXER, BoxerEntityRenderer::new);

        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearHudOverlay.register();
        FearSoundSystem.register();

        registerPotionColors();
        registerBowPredicates();
        registerKeys();
    }

    private void registerKeys() {
        journalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.crimsonmoon.journal",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.crimsonmoon"
        ));

        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.crimsonmoon.xray",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.crimsonmoon"
        ));

        ClientPlayNetworking.registerGlobalReceiver(JournalNetworking.RESPONSE, (client, handler, buf, sender) -> {
            BossJournalData data = BossJournalData.read(buf);
            client.execute(() -> {
                BossJournalScreen.latestData = data;
                if (client.currentScreen == null) {
                    client.setScreen(new BossJournalScreen());
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (journalKey.wasPressed()) {
                if (client.currentScreen == null && client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(JournalNetworking.REQUEST, PacketByteBufs.create());
                }
            }

            while (xrayKey != null && xrayKey.wasPressed()) {
                if (client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(new Identifier(CrimsonMoon.MOD_ID, "xray_toggle"), PacketByteBufs.create());
                }
            }
        });
    }

    private void registerBowPredicates() {
        ModelPredicateProviderRegistry.register(
            ModItems.BLOOD_BOW,
            new Identifier("pulling"),
            (stack, world, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0f : 0.0f
        );

        ModelPredicateProviderRegistry.register(
            ModItems.BLOOD_BOW,
            new Identifier("pull"),
            (stack, world, entity, seed) -> {
                if (entity == null || entity.getActiveItem() != stack) {
                    return 0.0f;
                }
                return (float) (stack.getMaxUseTime() - entity.getItemUseTimeLeft()) / 20.0f;
            }
        );
    }

    private void registerPotionColors() {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            Potion potion = PotionUtil.getPotion(stack);
            if (potion == ModPotions.BATTLE_POTION) {
                return 0xB22222; 
            }
            if (potion == ModPotions.BERSERKER_POTION) {
                return 0x8B0000; 
            }
            if (potion == ModPotions.HEROIC_POTION) {
                return 0xFFD700; 
            }
            return PotionUtil.getColor(stack);
        }, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW);

        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(NECKLACE_WORN_MODEL);
        });

        TrinketRendererRegistry.registerRenderer(ModItems.BLOOD_NECKLACE,
            (stack, slotReference, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {
                if (entity instanceof AbstractClientPlayerEntity player && contextModel instanceof PlayerEntityModel) {
                    @SuppressWarnings("unchecked")
                    PlayerEntityModel<AbstractClientPlayerEntity> playerModel = (PlayerEntityModel<AbstractClientPlayerEntity>) contextModel;

                    MinecraftClient mc = MinecraftClient.getInstance();
                    BakedModel wornModel = mc.getBakedModelManager().getModel(NECKLACE_WORN_MODEL);

                    matrices.push();
                    TrinketRenderer.translateToChest(matrices, playerModel, player);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                    matrices.scale(0.45F, 0.45F, 0.45F);
                    matrices.translate(-0.5F, 0.2F, -0.15F);

                    VertexConsumer vc = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutout(
                            new Identifier("crimsonmoon", "textures/item/blood_necklace_texture.png")
                        )
                    );

                    mc.getBlockRenderManager().getModelRenderer().render(
                        matrices.peek(), vc, null, wornModel, 1.0F, 1.0F, 1.0F, light, OverlayTexture.DEFAULT_UV
                    );
                    matrices.pop();
                }
            }
        );

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