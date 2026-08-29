package com.crimsonmoon.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Сгусток крові — боєприпас для BloodBowItem. Логіки в собі не несе (весь
 * функціонал — у луку та в BloodClotEntity), клас потрібен лише щоб мати
 * можливість дописати власний тултип.
 */
public class BloodClotItem extends Item {

    public BloodClotItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.crimsonmoon.blood_clot.tooltip").formatted(Formatting.GRAY));
    }
}
