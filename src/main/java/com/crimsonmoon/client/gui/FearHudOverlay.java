package com.crimsonmoon.client.gui;

import com.crimsonmoon.entity.BloodwalkerEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import java.util.List;

public class FearHudOverlay implements HudRenderCallback {
    private static final double DETECTION_RADIUS = 15.0;
    private static final Identifier FEAR_ICON = new Identifier("crimsonmoon", "textures/gui/fear_vignette.png");

    public static float currentFear = 0.0f;
    public static float currentPulse = 0.0f;

    @Override 
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null || client.world == null) {
            return;
        }

        Box searchBox = client.player.getBoundingBox().expand(DETECTION_RADIUS);
        List<BloodwalkerEntity> nearbyBloodwalkers = client.world.getEntitiesByClass(BloodwalkerEntity.class, searchBox, entity -> true);
        
        if (!nearbyBloodwalkers.isEmpty()) {
            double closestDistance = DETECTION_RADIUS;
            
            for (BloodwalkerEntity mob : nearbyBloodwalkers) {
                double distance = client.player.distanceTo(mob);
                if (distance < closestDistance) {
                    closestDistance = distance;
                }
            }
            
            float fearProcentage = (float) (1.0 - (closestDistance / DETECTION_RADIUS));
            
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            long currentTime = System.currentTimeMillis();
            float pulseSpeed = 300.0f - (fearProcentage * 200.0f);
            float pulse = (float)((Math.sin(currentTime / pulseSpeed) + 1.0) / 2.0);

            currentFear = fearProcentage;
            currentPulse = pulse;

            float finalAlpha = fearProcentage * pulse * 0.8f;
            
            context.getMatrices().push();
            
            if (fearProcentage > 0.7f) {
                context.getMatrices().translate(screenWidth / 2.0f, screenHeight / 2.0f, 0);
                float scale = 1.0f + (pulse * 0.15f); 
                context.getMatrices().scale(scale, scale, 1.0f);
                context.getMatrices().translate(-screenWidth / 2.0f, -screenHeight / 2.0f, 0);
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, finalAlpha);
            context.drawTexture(FEAR_ICON, 0, 0, -90, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            
            context.getMatrices().pop();
            
            int barWidth = 120;
            int barHeight = 8;
            int x = (screenWidth / 2) - (barWidth / 2);
            int y = 20;
            int filedWidth = (int) (barWidth * fearProcentage);

            context.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);
            context.fill(x, y, x + filedWidth, y + barHeight, 0xFF990000);

            String text = "Відчуття страху";
            int textWidth = client.textRenderer.getWidth(text);
            context.drawText(client.textRenderer, text, x + (barWidth - textWidth) / 2, y - 10, 0xFFAAAAAA, true);
        } else {
            currentFear = 0.0f;
            currentPulse = 0.0f;
        }
    }
}