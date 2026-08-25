package com.crimsonmoon.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class BloodNecklaceItem extends TrinketItem {
    
    public BloodNecklaceItem(Settings settings) {
        super(settings);
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return DropRule.KEEP;
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return false;
    }
}