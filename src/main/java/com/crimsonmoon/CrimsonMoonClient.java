package com.crimsonmoon;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.crimsonmoon.client.render.BloodwalkerEntityRenderer;
import com.crimsonmoon.client.render.WatcherEntityRenderer;
import com.crimsonmoon.client.render.NightmareEntityRenderer;
import com.crimsonmoon.client.render.ThorncallerEntityRenderer;
import com.crimsonmoon.client.render.BloodBoltEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

import com.crimsonmoon.client.gui.FearHudOverlay;
import com.crimsonmoon.client.sound.FearSoundSystem;

import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;


public class CrimsonMoonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BLOODWALKER, BloodwalkerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.NIGHTMARE, NightmareEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.THORNCALLER, ThorncallerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLOOD_BOLT, BloodBoltEntityRenderer::new);

        // HudRenderCallback.EVENT.register(new ParanoiaOverlay());
        HudRenderCallback.EVENT.register(new FearHudOverlay());
        FearSoundSystem.register();

        registerPotionColors();
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