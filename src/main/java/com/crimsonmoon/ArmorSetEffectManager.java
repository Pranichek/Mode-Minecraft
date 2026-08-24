package com.crimsonmoon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class ArmorSetEffectManager {

    private static final int EFFECT_DURATION = 60; // 3 сек. запасу для FIRE_RESISTANCE/REGENERATION

    private static final UUID HEALTH_BONUS_ID = UUID.fromString("b6e35d34-6d1b-4b0f-9a2a-2f1e6f9a11c3");
    private static final String HEALTH_BONUS_NAME = "crimsonmoon_armor_health_bonus";
    private static final double HEALTH_BONUS_AMOUNT = 16.0; // +8 сердець

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
    }

    private static void tick(ServerPlayerEntity player) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

        if (hasFullSet(player)) {
            applyOrRefresh(player, StatusEffects.FIRE_RESISTANCE, 1);
            applyOrRefresh(player, StatusEffects.REGENERATION, 1);

            if (maxHealth != null && maxHealth.getModifier(HEALTH_BONUS_ID) == null) {
                maxHealth.addPersistentModifier(new EntityAttributeModifier(
                    HEALTH_BONUS_ID,
                    HEALTH_BONUS_NAME,
                    HEALTH_BONUS_AMOUNT,
                    EntityAttributeModifier.Operation.ADDITION
                ));
            }
        } else {
            if (maxHealth != null && maxHealth.getModifier(HEALTH_BONUS_ID) != null) {
                maxHealth.removeModifier(HEALTH_BONUS_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    private static void applyOrRefresh(ServerPlayerEntity player, StatusEffect effect, int amplifier) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > EFFECT_DURATION / 2) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(
            effect, EFFECT_DURATION, amplifier, true, false, true
        ));
    }

    private static boolean hasFullSet(ServerPlayerEntity player) {
        ItemStack boots = player.getInventory().getArmorStack(0);
        ItemStack leggings = player.getInventory().getArmorStack(1);
        ItemStack chestplate = player.getInventory().getArmorStack(2);
        ItemStack helmet = player.getInventory().getArmorStack(3);

        if (boots.isEmpty() || leggings.isEmpty() || chestplate.isEmpty() || helmet.isEmpty()) {
            return false;
        }

        return boots.getItem() == ModItems.BLOOD_BOOTS &&
               leggings.getItem() == ModItems.BLOOD_LEGGINGS &&
               chestplate.getItem() == ModItems.BLOOD_CHESTPLATE &&
               helmet.getItem() == ModItems.BLOOD_HELMET;
    }
}