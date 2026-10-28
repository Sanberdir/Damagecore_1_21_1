package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class TitleTabRenderer {
    /** Любое значение, не совпадающее с TAB_NONE / TAB_ARMOR / TAB_POTION. */
    public static final int TAB_ID = 100;

    private static final int PANEL_W = 289;
    private static final int TEXT = 0xFFFFFFFF;       // если панель светлая, поменяйте на тёмный
    private static final int TEXT_DIM = 0xFFAAAAAA;

    private TitleTabRenderer() {}

    public static void render(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        Font font = mc.font;
        int cx = x + PANEL_W / 2;

        int rescue = p.getData(ModAttachments.TOME_RESCUE);
        int good   = p.getData(ModAttachments.TOME_GOOD);
        int bad    = p.getData(ModAttachments.TOME_BAD);
        String id = TomeTitles.idFor(rescue, good, bad);

        if (id == null) {
            gui.drawCenteredString(font, Component.translatable("gui.damagecore.title.none"),
                    cx, y + 80, TEXT_DIM);
            return;
        }

        // значок 16x16, увеличенный в 5 раз
        gui.pose().pushPose();
        gui.pose().translate(cx - 40, y + 14, 0);
        gui.pose().scale(5f, 5f, 1f);
        gui.blit(TomeTitles.icon(id), 0, 0, 0, 0, 16, 16, 16, 16);
        gui.pose().popPose();

        // название титула, масштаб 1.5
        gui.pose().pushPose();
        gui.pose().translate(cx, y + 104, 0);
        gui.pose().scale(1.5f, 1.5f, 1f);
        gui.drawCenteredString(font, Component.translatable("title.damagecore." + id), 0, 0, TEXT);
        gui.pose().popPose();

        gui.drawCenteredString(font, Component.translatable("gui.damagecore.title.outcome." + rescue),
                cx, y + 128, TEXT_DIM);
        gui.drawCenteredString(font, Component.translatable("gui.damagecore.title.light." + TomeTitles.lightLevel(good)),
                cx, y + 140, TEXT_DIM);
    }
}