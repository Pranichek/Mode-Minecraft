package com.crimsonmoon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;
import com.crimsonmoon.client.render.NightmareEntityRenderer;
import com.crimsonmoon.client.render.ThorncallerEntityRenderer;
import com.crimsonmoon.client.render.BloodBoltEntityRenderer;
import com.crimsonmoon.client.render.BloodClotEntityRenderer;
import com.crimsonmoon.client.render.MonsterikEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;

import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;


public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_BOLT, BloodBoltEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_CLOT, BloodClotEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MONSTERIK, MonsterikEntityRenderer::new);

        // HudRenderCallback.EVENT.register(new ParanoiaOverlay());
        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearHudOverlay.register();
        FearSoundSystem.register();

        registerPotionColors();
        registerBowPredicates();
    }

    // Предикати "pulling"/"pull" для blood_bow.json — саме за ними ресурспак
    // визначає, яку з 3 текстур натягу (pulling_0/1/2) показати замість
    // основної текстури лука, поки гравець тримає ПКМ.
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

    // Власні кольори для наших зіль (замість автоматичного змішування кольорів
    // ефектів). Для всіх інших зіль (ванільних і чужих) колір рахується як завжди.
    private void registerPotionColors() {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            Potion potion = PotionUtil.getPotion(stack);
            if (potion == ModPotions.BATTLE_POTION) {
                return 0xB22222; // вогняно-червоний - бій
            }
            if (potion == ModPotions.BERSERKER_POTION) {
                return 0x8B0000; // темно-кривавий - лють берсерка
            }
            if (potion == ModPotions.HEROIC_POTION) {
                return 0xFFD700; // золотий - геройське зілля
            }
            return PotionUtil.getColor(stack);
        }, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW);
    }
}