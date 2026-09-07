// DurationBarRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.gui.GuiGraphics;

/** Полоска "сколько времени осталось" под любым значком вкладки эффектов. */
public final class DurationBarRenderer {
    private DurationBarRenderer() {}

    public static void render(GuiGraphics gui, int x, int y, int width, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));

        gui.fill(x, y, x + width, y + EffectIconLayout.BAR_HEIGHT, 0xAA202020);

        int color = fraction > 0.5f ? 0xFF55FF55
                : fraction > 0.2f ? 0xFFFFFF55
                : 0xFFFF5555;

        int filled = Math.round(width * fraction);
        if (filled > 0) {
            gui.fill(x, y, x + filled, y + EffectIconLayout.BAR_HEIGHT, color);
        }
    }
}