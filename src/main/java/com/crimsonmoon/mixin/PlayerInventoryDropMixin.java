package com.crimsonmoon.mixin;

import com.crimsonmoon.item.NoDropRegistry;
import com.crimsonmoon.item.PendingItemStash;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryDropMixin {

    @Shadow public DefaultedList<ItemStack> main;
    @Shadow public DefaultedList<ItemStack> armor;
    @Shadow public DefaultedList<ItemStack> offHand;
    @Shadow public PlayerEntity player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void crimsonmoon$keepProtectedItems(CallbackInfo ci) {
        stripProtected(main);
        stripProtected(armor);
        stripProtected(offHand);
    }

    private void stripProtected(DefaultedList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (NoDropRegistry.isProtected(stack)) {
                PendingItemStash.add(this.player.getUuid(), stack);
                list.set(i, ItemStack.EMPTY);
            }
        }
    }
}