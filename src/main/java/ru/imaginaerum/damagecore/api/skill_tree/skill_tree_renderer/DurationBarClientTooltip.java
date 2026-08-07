package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Клиентский рендер DurationBarTooltip — горизонтальная полоска, тающая по мере истечения времени эффекта. */
public final class DurationBarClientTooltip implements ClientTooltipComponent {

    private static final int BAR_WIDTH  = 90;
    private static final int BAR_HEIGHT = 3;
    private static final int TOP_PAD    = 3;
    private static final int BOTTOM_PAD = 2;

    private final float progress;
    private final Component name;
    private final int amplifier;

    public DurationBarClientTooltip(DurationBarTooltip data) {
        this.progress  = Mth.clamp(data.progress(), 0f, 1f);
        this.name      = data.name();
        this.amplifier = data.amplifier();
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    @Override
    public int getHeight() {
        // высота текста (9) + отступ (2) + полоска + паддинги
        return 9 + 2 + BAR_HEIGHT + TOP_PAD + BOTTOM_PAD;
    }

    @Override
    public int getWidth(Font font) {
        String label = name.getString() + " " + toRoman(amplifier);
        return Math.max(BAR_WIDTH, font.width(label));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics gui) {
        // текст: "Название Уровень"
        String label = name.getString() + " " + toRoman(amplifier);
        gui.drawString(font, label, x, y, 0xFFFFFF, true);

        // полоска ниже текста
        int barY = y + 9 + 2 + TOP_PAD;

        gui.fill(x, barY, x + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF3A3A3A);

        int filled = Math.round(BAR_WIDTH * progress);
        if (filled > 0) {
            int color = progress > 0.34f ? 0xFF55C95F : 0xFFD24B4B;
            gui.fill(x, barY, x + filled, barY + BAR_HEIGHT, color);
        }
    }
}