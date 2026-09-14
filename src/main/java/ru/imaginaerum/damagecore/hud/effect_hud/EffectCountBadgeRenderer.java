package ru.imaginaerum.damagecore.hud.effect_hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Рисует число-бейдж в углу иконки — количество эффектов,
 * сгруппированных под одним источником/типом наложения.
 */
public final class EffectCountBadgeRenderer {

    public enum Corner { BOTTOM_RIGHT, BOTTOM_LEFT }

    private EffectCountBadgeRenderer() {}

    /** Старое поведение (правый нижний угол) — не трогаем существующих вызывающих. */
    public static void render(GuiGraphics gui, Font font, int iconX, int iconY, int iconSize, int count) {
        render(gui, font, iconX, iconY, iconSize, count, Corner.BOTTOM_RIGHT);
    }

    public static void render(GuiGraphics gui, Font font, int iconX, int iconY, int iconSize, int count, Corner corner) {
        if (count <= 1) return;

        String text = String.valueOf(count);
        int textWidth = font.width(text);

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0.0, 0.0, 250.0);

        int drawX = (corner == Corner.BOTTOM_LEFT)
                ? iconX + 1
                : iconX + iconSize - textWidth - 1;
        int drawY = iconY + iconSize - font.lineHeight + 1;

        gui.drawString(font, text, drawX, drawY, 0xFFFFFF, true);
        pose.popPose();
    }
}