package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Config;

public class ThirstBarElement {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/hud/damage_core_hud.png");

    private static final int TEXTURE_W = 160;
    private static final int TEXTURE_H = 208;

    // Точные координаты и размеры значка капли по твоей сетке (7х9 пикселей)
    private static final int ICON_SRC_X = 17;
    private static final int ICON_SRC_Y = 119;
    private static final int ICON_W     = 7;
    private static final int ICON_H     = 9;

    // Точные координаты заполнения воды (12x8 пикселей)
    private static final int FILL_SRC_X  = 20;
    private static final int FILL_SRC_Y  = 109;
    private static final int FILL_W      = 12;
    private static final int FILL_H      = 8;

    // Точные координаты пустой подложки (16x10 пикселей)
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 156;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    public static float thirst = 20f;
    public static final float MAX_THIRST = 20f;

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null || !Config.enableThirst) return;

        int screenH    = mc.getWindow().getGuiScaledHeight();
        int screenW    = mc.getWindow().getGuiScaledWidth();
        int hotbarLeft = screenW / 2 - 50;
        int heartsY    = screenH - 49;

        // Позиция значка капли на экране (на одном уровне с окорочком)
        int screenX = hotbarLeft - 38 + 18 - 3 - 4 - 3 + 11;
        int screenY = heartsY + 7 - 2 + 8 + 4;

        // 1. РИСУЕМ ЗНАЧОК КАПЛИ (Нижний элемент)
        gui.blit(TEXTURE, screenX, screenY, ICON_SRC_X, ICON_SRC_Y, ICON_W, ICON_H, TEXTURE_W, TEXTURE_H);

        // Базовая точка для верхних полосок (размер 16х10).
        int barX = screenX + 3;
        int barY = screenY - EMPTY_H - 2;

        // 2. РИСУЕМ ПУСТУЮ ПОДЛОЖКУ (16х10) над значком капли
        gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X, EMPTY_SRC_Y, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);

        // 3. РИСУЕМ ЗАПОЛНЕННУЮ ЧАСТЬ ВОДЫ (12х8) внутри подложки
        int fillH = Math.round(FILL_H * (thirst / MAX_THIRST));
        if (fillH > 0) {
            int cut = FILL_H - fillH;

            // Вкладываем полоску 12х8 внутрь подложки 16х10. Сдвиг +2 вправо и +1 вниз от barX/barY
            int screenFillX = barX + 2;
            int screenFillY = barY + 1 + cut;

            gui.blit(TEXTURE, screenFillX, screenFillY, FILL_SRC_X, FILL_SRC_Y + cut, FILL_W, fillH, TEXTURE_W, TEXTURE_H);
        }
    }
}
