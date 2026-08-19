package com.crimsonmoon.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.network.ClientPlayerEntity;

public class ParanoiaOverlay implements HudRenderCallback {
    private static final int MAX_COUNT = 64;

    @Override
    public void onHudRender(DrawContext context, float tickDelta){
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null){
            return;
        }
        int dirtCount = countItems(client.player);
        if (dirtCount > 0){
            int barWidth = 100;
            int barHeight = 10;
            int x = 10;
            int y = 10;


            float fillPercentage = Math.min((float) dirtCount / MAX_COUNT, 1.0f);
            int fillWidth = (int)(barWidth * fillPercentage);
            

            context.fill(x, y, x + barWidth, y + barHeight, 0xFF555555);
            context.fill(x, y, x + fillWidth, y + barHeight, 0xFFAA0000);
            String text = dirtCount + " / " + MAX_COUNT;
            context.drawText(client.textRenderer, text, x + barWidth + 5, y + 1, 0xFFFFFF, true);
        }    
    }

    private int countItems(ClientPlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.DIRT)) {
                count += stack.getCount();
            } 
        }
        return count;
    }
}
