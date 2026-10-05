package com.crimsonmoon.client.gui;

import com.crimsonmoon.BossJournalData;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class BossJournalScreen extends Screen {

    public static BossJournalData latestData = new BossJournalData();

    private boolean english = false;
    private boolean showCoords = false;

    private static final Identifier ICON_NIGHTMARE = new Identifier("crimsonmoon", "textures/gui/journal/nightmare.png");
    private static final Identifier ICON_BOXER = new Identifier("crimsonmoon", "textures/gui/journal/boxer.png");
    private static final Identifier ICON_THORNCALLER = new Identifier("crimsonmoon", "textures/gui/journal/thorncaller.png");
    private static final Identifier ICON_MONSTERIK = new Identifier("minecraft", "textures/item/nether_star.png");
    private static final Identifier ICON_MONOLIT = new Identifier("minecraft", "textures/item/amethyst_shard.png");

    public BossJournalScreen() {
        super(Text.literal("Журнал Кривавого Місяця"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(english ? "UA" : "EN"),
            b -> { this.english = !this.english; this.clearAndInit(); }
        ).dimensions(this.width - 50, 10, 40, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(english ? "Show coordinates" : "Показати координати"),
            b -> { this.showCoords = true; this.clearAndInit(); }
        ).dimensions(this.width / 2 - 90, this.height - 28, 180, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFF5555);

        int x = this.width / 2 - 190;
        int y = 28;
        int rowH = 40;

        // --- Етап 1: моноліт ---
        String monolitCoords = "";
        if (showCoords && latestData.monolitPlaced && !latestData.isCurseActive) {
            monolitCoords = "  [" + latestData.monolitX + ", " + latestData.monolitY + ", " + latestData.monolitZ + "]";
        }
        
        String monolitStatus = latestData.isCurseActive 
            ? (english ? " [DESTROYED]" : " [ЗРУЙНОВАНО]") 
            : (english ? " [INTACT]" : " [ЦІЛИЙ]");

        drawRow(ctx, x, y, ICON_MONOLIT,
            (english ? "Stage 1: Monolith" : "Етап 1: Моноліт") + monolitStatus,
            english ? "A cursed building holds the Monolith. Break it to begin the curse."
                    : "У проклятій будівлі стоїть Моноліт. Зруйнуй його, щоб почати прокляття.",
            (english ? "Location: On the surface" : "Оселя: На поверхні") + monolitCoords,
            latestData.isCurseActive); // Передаємо статус, щоб текст став зеленим
        y += rowH;

        // --- Жах (Nightmare) ---
        drawRow(ctx, x, y, ICON_NIGHTMARE,
            (english ? "Nightmare" : "Жах") + statusText(latestData.nightmareDefeated),
            english ? "A shadow that hunts underground. It does not forgive the light."
                    : "Тінь, що полює під землею. Воно не пробачає світла.",
            english ? "Location: Caves (go deep underground)" : "Оселя: Печери (спустись глибоко під землю)",
            latestData.nightmareDefeated);
        y += rowH;

        // --- Боксер ---
        String boxerCoords = "";
        if (showCoords && latestData.arenaPlaced) {
            boxerCoords = "  [" + latestData.arenaX + ", " + latestData.arenaY + ", " + latestData.arenaZ + "]";
        }
        drawRow(ctx, x, y, ICON_BOXER,
            (english ? "The Boxer" : "Боксер") + statusText(latestData.boxerDefeated),
            english ? "A colossus of fists. Walls will not save you from its wrath."
                    : "Колос із кулаків. Стіни не врятують від його люті.",
            (english ? "Location: Colosseum in the desert" : "Оселя: Колізей у пустелі") + boxerCoords,
            latestData.boxerDefeated);
        y += rowH;

        // --- Терновик (Thorncaller) ---
        drawRow(ctx, x, y, ICON_THORNCALLER,
            (english ? "Thorncaller" : "Терновик") + statusText(latestData.thorncallerDefeated),
            english ? "It watches from the canopy. The jungle itself is its weapon."
                    : "Воно стежить із крон. Самі джунглі — його зброя.",
            english ? "Location: Jungle (find any jungle)" : "Оселя: Джунглі (знайди будь-які джунглі)",
            latestData.thorncallerDefeated);
        y += rowH;

        // --- Монстерік (фінал) ---
        drawRow(ctx, x, y, ICON_MONSTERIK,
            (english ? "Monsterik — Final" : "Монстерік — Фінал") + statusText(latestData.monsterikDefeated),
            english ? "The heart of the curse. Defeat the three to open the way to it."
                    : "Серце прокляття. Здолай трьох, щоб відкрити шлях до нього.",
            english ? "Location: Beyond the portal" : "Оселя: За порталом",
            latestData.monsterikDefeated);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private String statusText(boolean defeated) {
        if (defeated) return english ? " [DEFEATED]" : " [ПЕРЕМОЖЕНО]";
        return english ? " [ALIVE]" : " [ЖИВИЙ]";
    }

    private void drawRow(DrawContext ctx, int x, int y, Identifier icon,
                         String name, String desc, String place, Boolean defeated) {
        ctx.drawTexture(icon, x, y, 0, 0, 32, 32, 32, 32);
        int textX = x + 40;

        Formatting nameColor = Formatting.WHITE;
        if (defeated != null) {
            nameColor = defeated ? Formatting.GREEN : Formatting.WHITE;
        }
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal(name).formatted(nameColor), textX, y, 0xFFFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal(desc).formatted(Formatting.GRAY), textX, y + 11, 0xFFAAAAAA);

        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal(place).formatted(Formatting.YELLOW), textX, y + 22, 0xFFFFFF55);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}