package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;

public class HungerBarElement {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/hud/damage_core_hud.png");

    private static final int TEXTURE_W = 160;
    private static final int TEXTURE_H = 208;

    // Точные координаты и размеры значка окорочка (9х9 пикселей)
    private static final int ICON_SRC_X = 0;
    private static final int ICON_SRC_Y = 119;
    private static final int ICON_W     = 9;
    private static final int ICON_H     = 9;

    // Координаты для версии значка под эффектом "Голод"
    private static final int ICON_SRC_HUNGER = 141;

    // Точные координаты заполнения голода (12x8 пикселей)
    private static final int FILL_SRC_X  = 4;
    private static final int FILL_SRC_Y  = 109;
    private static final int FILL_W      = 12;
    private static final int FILL_H      = 8;

    // Полоска под эффектом "Голод"
    private static final int FILL_SRC_X_HUNGER = 4;
    private static final int FILL_SRC_Y_HUNGER = 131;

    // Точные координаты пустой подложки (16x10)
    private static final int EMPTY_SRC_X        = 0;
    private static final int EMPTY_SRC_Y        = 156;
    private static final int EMPTY_SRC_X_HUNGRY = 16;
    private static final int EMPTY_SRC_Y_HUNGRY = 156;
    private static final int EMPTY_W            = 16;
    private static final int EMPTY_H            = 10;

    // Точные координаты насыщенности (16x10)
    private static final int SAT_SRC_X = 0;
    private static final int SAT_SRC_Y = 166;
    private static final int SAT_W     = 16;
    private static final int SAT_H     = 10;

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        boolean isHungry = mc.player.hasEffect(MobEffects.HUNGER);

        FoodData food = mc.player.getFoodData();
        float hunger     = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        float maxHunger  = 20f;
        float maxSat     = 20f;

        int screenH    = mc.getWindow().getGuiScaledHeight();
        int screenW    = mc.getWindow().getGuiScaledWidth();
        int hotbarLeft = screenW / 2 - 50;
        int heartsY    = screenH - 49;

        // Базовая позиция значка окорочка на экране
        int screenX = hotbarLeft - 19 - ICON_W - 3 - 10;
        int screenY = heartsY + 7 - 2 + 12;

        // 1. РИСУЕМ ЗНАЧОК ОКОРОЧКА
        int iconSrcX = ICON_SRC_X;
        int iconSrcY = isHungry ? ICON_SRC_HUNGER : ICON_SRC_Y;
        gui.blit(TEXTURE, screenX, screenY, iconSrcX, iconSrcY, ICON_W, ICON_H, TEXTURE_W, TEXTURE_H);

        // ИСПРАВЛЕНО: Базовая точка для ВСЕХ верхних полосок (размер 16х10).
        // Сдвинута на 4 пикселя правее значка и поднята на 2 пикселя над ним (с учётом высоты 10px)
        int barX = screenX + 4;
        int barY = screenY - EMPTY_H - 2;

        // 2. РИСУЕМ ПУСТУЮ ПОДЛОЖКУ (16х10) над значком
        if (isHungry) {
            gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X_HUNGRY, EMPTY_SRC_Y_HUNGRY, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);
        } else {
            gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X, EMPTY_SRC_Y, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);
        }

        // 3. РИСУЕМ ЗАПОЛНЕННУЮ ЧАСТЬ ГОЛОДА (12х8) внутри подложки
        int fillH = Math.round(FILL_H * (hunger / maxHunger));
        if (fillH > 0) {
            int cut = FILL_H - fillH;
            int fillSrcX = isHungry ? FILL_SRC_X_HUNGER : FILL_SRC_X;
            int fillSrcY = isHungry ? FILL_SRC_Y_HUNGER + cut : FILL_SRC_Y + cut;

            // Вкладываем полоску 12х8 внутрь подложки 16х10. Сдвиг +2 вправо и +1 вниз
            int screenFillX = barX + 2;
            int screenFillY = barY + 1 + cut;

            gui.blit(TEXTURE, screenFillX, screenFillY, fillSrcX, fillSrcY, FILL_W, fillH, TEXTURE_W, TEXTURE_H);
        }

        // 4. РИСУЕМ НАСЫЩЕННОСТЬ ПОВЕРХ (16х10)
        float satClamped = Math.min(saturation, maxSat);
        int satH = Math.round(SAT_H * (satClamped / maxSat));
        if (satH > 0) {
            int cut = SAT_H - satH;

            // Ложится один в один на пустую подложку
            int screenSatX = barX;
            int screenSatY = barY + cut;

            gui.blit(TEXTURE, screenSatX, screenSatY, SAT_SRC_X, SAT_SRC_Y + cut, SAT_W, satH, TEXTURE_W, TEXTURE_H);
        }
    }
}
