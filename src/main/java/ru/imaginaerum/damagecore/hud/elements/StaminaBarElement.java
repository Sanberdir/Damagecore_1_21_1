package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class StaminaBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 40;
    private static final int BAR_Y = 33;
    private static final int BAR_W = 52;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 0;
    private static final int TEXTURE_Y_EMPTY = 72;

    private static final int FILLED_TEX_X = 2;
    private static final int FILLED_TEX_Y = 61;
    private static final int FILLED_TEX_W = 28;
    private static final int FILLED_TEX_H = 4;

    private static final int HUD_TEXTURE_WIDTH  = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;

    private static final int TEXTURE_X_FLASH = 32;
    private static final int TEXTURE_Y_FLASH = 78;
    private static final int FLASH_DURATION  = 3;

    private static float lastStamina  = -1f;
    private static int   flashTicks   = 0;
    private static long  lastGameTime = -1L;

    public static void render(GuiGraphics gui) {
        if (!Config.showStaminaHud) return;

        float stamina    = StaminaManager.getStamina();
        float maxStamina = StaminaManager.getMaxStamina();

        long now = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : System.currentTimeMillis() / 50;

        if (lastGameTime != now) {
            lastGameTime = now;

            if (lastStamina >= 0 && stamina < lastStamina) {
                flashTicks = FLASH_DURATION;
            }
            if (flashTicks > 0) flashTicks--;

            lastStamina = stamina;
        }

        boolean useAlt = flashTicks > 0;

        int emptyTexX = useAlt ? TEXTURE_X_FLASH : TEXTURE_X;
        int emptyTexY = useAlt ? TEXTURE_Y_FLASH : TEXTURE_Y_EMPTY;

        float widthScale = maxStamina / StaminaManager.BASE_STAMINA;
        int scaledBarW   = Math.round(BAR_W * widthScale);

        renderEmptyBar(gui, BAR_X, BAR_Y, scaledBarW, BAR_H, emptyTexX, emptyTexY);

        float fraction = stamina / maxStamina;
        int maxFilledW = scaledBarW - 4;
        int filledW    = Math.round(maxFilledW * fraction);

        if (filledW > 0) {
            renderFilledBar(gui,
                    BAR_X + 2,
                    BAR_Y + 1,
                    filledW,
                    BAR_H - 2,
                    FILLED_TEX_X,
                    FILLED_TEX_Y,
                    maxFilledW);
        }
    }

    static void renderEmptyBar(GuiGraphics gui, int barX, int barY, int barW, int barH,
                               int texX, int texY) {
        int drawX = barX;

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += EDGE_WIDTH;

        int midWidth = barW - EDGE_WIDTH * 2;
        int midTexX  = texX + EDGE_WIDTH;
        int midTexW  = TEXTURE_BAR_WIDTH - EDGE_WIDTH * 2;
        if (midWidth > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midWidth, barH,
                    midTexX, texY,
                    midTexW, barH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midWidth;
        }

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
    }

    static void renderFilledBar(GuiGraphics gui, int barX, int barY, int barW, int barH,
                                int texX, int texY, int maxFilledW) {
        int drawX = barX;
        int filledEdgeWidth = Math.min(EDGE_WIDTH, FILLED_TEX_W / 2);
        int midTexW = FILLED_TEX_W - filledEdgeWidth * 2;

        // Левый край
        int leftW = Math.min(filledEdgeWidth, barW);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                leftW, barH,
                texX, texY,
                leftW, FILLED_TEX_H,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += leftW;
        barW  -= leftW;
        if (barW <= 0) return;

        // Резервируем место под правый скос только если
        // заполненная часть достигает правого края maxFilledW
        int distanceFromEnd = maxFilledW - (leftW + barW);
        int rightEdgeVisible = Math.max(0,
                Math.min(filledEdgeWidth, filledEdgeWidth - distanceFromEnd));

        // Середина
        int midScreenW = Math.max(0, barW - rightEdgeVisible);
        if (midScreenW > 0 && midTexW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midScreenW, barH,
                    texX + filledEdgeWidth, texY,
                    midTexW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midScreenW;
            barW  -= midScreenW;
        }

        // Правый скос
        if (rightEdgeVisible > 0 && barW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    rightEdgeVisible, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    rightEdgeVisible, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}