package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HudBase {

    public static void render(GuiGraphics gui) {
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, 8, 4, 0, 0, 40, 40, 160, 208);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, 5, 1, 86, 0, 46, 46, 160, 208);

        renderXpFill(gui);
        renderXpLevel(gui);
    }

    private static void renderXpFill(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float xpProgress = mc.player.experienceProgress;

        int fullHeight = 46;
        int fullWidth = 46;

        int filledHeight = Math.round(fullHeight * xpProgress);
        if (filledHeight <= 0) return;

        int srcX = 40;
        int srcY = fullHeight - filledHeight;
        int destX = 5;
        int destY = 1 + (fullHeight - filledHeight);

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, destX, destY, srcX, srcY, fullWidth, filledHeight, 160, 208);
    }

    private static void renderXpLevel(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int level = mc.player.experienceLevel;
        String text = String.valueOf(level);

        // Центр спрайта: x=8, y=4, размер 40x40
        int spriteX = 8;
        int spriteY = 4;
        int spriteW = 40;
        int spriteH = 40;

        int textW = mc.font.width(text);
        int textH = mc.font.lineHeight;

        int textX = spriteX + (spriteW - textW) / 2 + 1;
        int textY = spriteY + (spriteH - textH) / 2 + 1;

        gui.drawString(mc.font, text, textX, textY, 0x80FF20, false);
    }
}