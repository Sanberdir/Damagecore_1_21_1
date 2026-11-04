package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class TitleTabRenderer {
    /** Любое значение, не совпадающее с TAB_NONE / TAB_ARMOR / TAB_POTION. */
    public static final int TAB_ID = 100;

    private static final int PANEL_W = 289;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFAAAAAA;

    // строка значков
    private static final int ICON = 16;
    private static final int GAP = 6;
    private static final int ROW_Y = 10;
    private static final int ROW_W = TomeTitles.ALL.length * ICON + (TomeTitles.ALL.length - 1) * GAP;

    /** Какой титул сейчас открыт для просмотра (null = полученный). */
    private static String selected = null;

    private TitleTabRenderer() {}

    /** Вызывать при открытии вкладки, чтобы снова показывался полученный титул. */
    public static void resetSelection() {
        selected = null;
    }

    private static final int COLS = 4;
    private static final int ROWS = 4;
    private static final int CELL_W = 62;
    private static final int CELL_H = 24;
    private static final int CELL_GAP = 3;
    private static final float TEXT_SCALE = 0.61f;

    private static final int NAME_Y = 17;      // название сверху
    private static final int GRID_Y = 34;      // сетка под названием

    private static final int GRID_W = COLS * CELL_W + (COLS - 1) * CELL_GAP;

    private static final int GRID_BOTTOM = GRID_Y + ROWS * CELL_H + (ROWS - 1) * CELL_GAP; // 127
    private static int cellX(int x, int i) {
        return x + (PANEL_W - GRID_W) / 2 + (i % COLS) * (CELL_W + CELL_GAP);
    }

    private static int cellY(int y, int i) {
        return y + GRID_Y + (i / COLS) * (CELL_H + CELL_GAP);
    }

    private static String earnedId(Player p) {
        return TomeTitles.idFor(
                p.getData(ModAttachments.TOME_RESCUE),
                p.getData(ModAttachments.TOME_GOOD),
                p.getData(ModAttachments.TOME_BAD));
    }

    /** Вызывать из mouseClicked экрана. Возвращает true, если клик попал в значок. */
    public static boolean mouseClicked(int x, int y, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        for (int i = 0; i < TomeTitles.ALL.length; i++) {
            int cx = cellX(x, i), cy = cellY(y, i);
            if (mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H) {
                selected = TomeTitles.ALL[i];
                return true;
            }
        }
        return false;
    }

    public static void render(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        Font font = mc.font;
        int cx = x + PANEL_W / 2;

        int rescue = p.getData(ModAttachments.TOME_RESCUE);
        int good   = p.getData(ModAttachments.TOME_GOOD);
        String earned = earnedId(p);
        String shown = selected != null ? selected : earned;

        // ---- сетка с названиями ----
        String hovered = null;
        for (int i = 0; i < TomeTitles.ALL.length; i++) {
            String id = TomeTitles.ALL[i];
            int cx0 = cellX(x, i), cy0 = cellY(y, i);

            boolean isSelected = id.equals(shown);
            boolean isHover = mouseX >= cx0 && mouseX < cx0 + CELL_W
                    && mouseY >= cy0 && mouseY < cy0 + CELL_H;
            if (isHover) hovered = id;

            // подложка
            if (isSelected) {
                gui.fill(cx0 - 1, cy0 - 1, cx0 + CELL_W + 1, cy0 + CELL_H + 1, 0xFFFFD866);
                gui.fill(cx0, cy0, cx0 + CELL_W, cy0 + CELL_H, 0xFF8A8A8A);
            } else if (isHover) {
                gui.fill(cx0, cy0, cx0 + CELL_W, cy0 + CELL_H, 0x66FFFFFF);
            } else {
                gui.fill(cx0, cy0, cx0 + CELL_W, cy0 + CELL_H, 0x66000000);
            }

            String name = Component.translatable("title.damagecore." + id).getString();
            int maxW = CELL_W - 4;
            float scale = TEXT_SCALE;
// ширина в «нескалированных» единицах, которая влезает в плашку
            int limit = (int) (maxW / scale);

// переносим по словам
            List<FormattedCharSequence> lines = font.split(Component.literal(name), limit);

// если какое-то слово шире строки, сжимаем масштаб так, чтобы влезло
            int widest = 0;
            for (FormattedCharSequence l : lines) widest = Math.max(widest, font.width(l));
            if (widest > limit) scale = scale * limit / widest;

// максимум две строки
            int count = Math.min(lines.size(), 2);
            int color = isSelected ? 0xFFFFFFFF : (isHover ? 0xFFDDDDDD : 0xFF777777);

            float lineH = 9 * scale;
            float totalH = count * lineH - scale; // без нижнего зазора
            float startY = cy0 + (CELL_H - totalH) / 2f;

            for (int li = 0; li < count; li++) {
                FormattedCharSequence l = lines.get(li);
                float tw = font.width(l) * scale;
                gui.pose().pushPose();
                gui.pose().translate(cx0 + (CELL_W - tw) / 2f, startY + li * lineH, 0);
                gui.pose().scale(scale, scale, 1f);
                gui.drawString(font, l, 0, 0, color, false);
                gui.pose().popPose();
            }
        }

        // ---- выбранный титул по центру ----
        if (shown == null) {
            gui.drawCenteredString(font, Component.translatable("gui.damagecore.title.none"),
                    cx, y + NAME_Y, TEXT_DIM);
        } else {
            gui.pose().pushPose();
            gui.pose().translate(cx, y + NAME_Y, 0);
            gui.pose().scale(0.75f, 0.75f, 1f);
            gui.drawCenteredString(font, Component.translatable("title.damagecore." + shown), 0, 0, TEXT);
            gui.pose().popPose();
        }

        // подсказка с названием при наведении
        if (hovered != null) {
            gui.renderTooltip(font, Component.translatable("title.damagecore." + hovered), mouseX, mouseY);
        }
    }
}