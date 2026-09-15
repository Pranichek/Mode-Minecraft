package com.crimsonmoon.client.gui;

import com.crimsonmoon.FearManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * HUD-шкала страха.
 *
 * Значение НЕ считается на клиенте — оно приходит с сервера по сети (см.
 * {@link FearManager#SYNC_CHANNEL}, регистрация приёма — в {@link #register()}).
 * Это важно: страх — это реальный игровой ресурс, которым платят способности
 * Кровавого посоха/булавы (см. {@code BloodStaffItem}, {@code BloodMaceItem}),
 * и HUD обязан показывать именно то, что реально потрачено/накоплено на
 * сервере, а не собственную клиентскую догадку.
 *
 * Полоса — горизонтальная, справа от хотбара.
 */
public class FearHudOverlay implements HudRenderCallback {

    private static final Identifier FEAR_ICON = new Identifier("crimsonmoon", "textures/gui/fear_vignette.png");

    /** Реальное значение 0..100, приходит с сервера — см. {@link #register()}. */
    public static float currentFear = 0.0f;
    public static float currentPulse = 0.0f;

    /** Регистрирует приём синхронизации страха с сервера. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FearManager.SYNC_CHANNEL, (client, handler, buf, responseSender) -> {
            float value = buf.readFloat();
            client.execute(() -> currentFear = value);
        });
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            return;
        }

        // --- ПЕРЕВІРКА НА НАЯВНІСТЬ ПРОКЛЯТТЯ (НАМИСТА) ---
        boolean isCursed = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(client.player)
            .map(comp -> comp.isEquipped(com.crimsonmoon.ModItems.BLOOD_NECKLACE))
            .orElse(false);

        if (!isCursed) {
            return; // Якщо прокляття знято, припиняємо малювати шкалу
        }
        // --------------------------------------------------

        float fearPercentage = currentFear / FearManager.MAX_FEAR;
        if (fearPercentage <= 0.0f) {
            currentPulse = 0.0f;
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        long currentTime = System.currentTimeMillis();
        float pulseSpeed = 300.0f - (fearPercentage * 200.0f);
        float pulse = (float) ((Math.sin(currentTime / pulseSpeed) + 1.0) / 2.0);
        currentPulse = pulse;

        float finalAlpha = fearPercentage * pulse * 0.8f;

        context.getMatrices().push();

        if (fearPercentage > 0.7f) {
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

        // === полоса — горизонтальная, справа от хотбара ===
        int barWidth = 60;
        int barHeight = 8;
        int hotbarHalfWidth = 91;      // стандартная половина ширины хотбара в GUI-координатах
        int offhandSlotClearance = 29; // чтобы не залезать на слот доп. руки рядом с хотбаром

        int x = (screenWidth / 2) + hotbarHalfWidth + offhandSlotClearance;
        int y = screenHeight - 22 + (22 - barHeight) / 2; // по центру высоты хотбара
        int filledWidth = (int) (barWidth * fearPercentage);

        context.fill(x, y, x + barWidth, y + barHeight, 0xFF444444);
        context.fill(x, y, x + filledWidth, y + barHeight, 0xFF990000);

        String text = "Страх";
        int textWidth = client.textRenderer.getWidth(text);
        context.drawText(client.textRenderer, text, x + (barWidth - textWidth) / 2, y - 10, 0xFFAAAAAA, true);
    }
}