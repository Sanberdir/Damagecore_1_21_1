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

    // Значок голода — фон
    private static final int ICON_SRC_X = 0;
    private static final int ICON_SRC_Y = 109;
    private static final int ICON_W     = 18;
    private static final int ICON_H     = 20;

    // Заполненная часть (убывает сверху вниз)
    private static final int FILL_SRC_X = 4;
    private static final int FILL_SRC_Y = 109;
    private static final int FILL_W     = 16;
    private static final int FILL_H     = 10;

    // Для голода
    private static final int ICON_SRC_HUNGER  = 131;
    private static final int FILL_SRC_X_HUNGER = 4;
    private static final int FILL_SRC_Y_HUNGER = 131;

    // Пустая часть
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 156;
    // С голодом
    private static final int EMPTY_SRC_X_HUNGRY = 16;
    private static final int EMPTY_SRC_Y_HUNGRY = 156;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    // Насыщенность
    private static final int SAT_SRC_X = 0;
    private static final int SAT_SRC_Y = 166;
    private static final int SAT_W     = 16;
    private static final int SAT_H     = 10;

    private static final int BAR_OFFSET_X = 2;
    private static final int BAR_OFFSET_Y = 0;  // ← ПОДНЯЛИ ПОЛОСКИ НА 1 ПИКСЕЛЬ

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

        int screenX = hotbarLeft - 19 - ICON_W;
        int screenY = heartsY + 7;  // значок остался на месте

        // 1. Иконка-фон
        int iconSrcY = isHungry ? ICON_SRC_HUNGER - 1 : ICON_SRC_Y - 1;
        gui.blit(TEXTURE, screenX, screenY, ICON_SRC_X, iconSrcY, ICON_W, ICON_H, TEXTURE_W, TEXTURE_H);

        int barX = screenX + BAR_OFFSET_X;
        int barY = screenY + BAR_OFFSET_Y;  // теперь barY = screenY - 1

        // 2. Пустая полоска
        if (isHungry) {
            gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X_HUNGRY, EMPTY_SRC_Y_HUNGRY, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);
        }
        else {
            gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X, EMPTY_SRC_Y, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);
        }
        // 3. Заполненная часть — убывает сверху вниз
        int fillH = Math.round(FILL_H * (hunger / maxHunger));
        if (fillH > 0) {
            int cut = FILL_H - fillH;
            int fillSrcX = isHungry ? FILL_SRC_X_HUNGER : FILL_SRC_X;
            int fillSrcY = isHungry ? FILL_SRC_Y_HUNGER + cut - 1 : FILL_SRC_Y + cut - 1;

            // смещение твоего fill: вправо на 2, вниз на 1
            int screenFillX = barX + 2;
            int screenFillY = barY + cut;

            gui.blit(TEXTURE, screenFillX, screenFillY, fillSrcX, fillSrcY, FILL_W, fillH, TEXTURE_W, TEXTURE_H);
        }

        // 4. Насыщенность поверх
        float satClamped = Math.min(saturation, maxSat);
        int satH = Math.round(SAT_H * (satClamped / maxSat));
        if (satH > 0) {
            int cut = SAT_H - satH;
            gui.blit(TEXTURE, barX, barY + cut, SAT_SRC_X, SAT_SRC_Y + cut, SAT_W, satH, TEXTURE_W, TEXTURE_H);
        }
    }
}