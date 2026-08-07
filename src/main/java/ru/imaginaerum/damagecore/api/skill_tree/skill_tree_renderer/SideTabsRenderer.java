package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.ArmorTabRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.PotionTabRenderer;

public final class SideTabsRenderer {

    public static final int TAB_NONE   = -1;
    public static final int TAB_ARMOR  = 0;
    public static final int TAB_POTION = 1;

    private SideTabsRenderer() {}

    public static void render(GuiGraphics gui,
                              int panelScreenX,
                              int panelScreenY,
                              int activeTab,
                              int mouseX,
                              int mouseY,
                              ItemStack previewArmorStack) {

        int areaX = panelScreenX + Render.PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + Render.PANEL_DRAW_OFFSET_Y_IN_PANEL;

        Minecraft mc = Minecraft.getInstance();

        switch (activeTab) {
            case TAB_ARMOR -> ArmorTabRenderer.render(gui, areaX, areaY, mc, mouseX, mouseY, previewArmorStack);
            case TAB_POTION -> PotionTabRenderer.render(gui, areaX, areaY, mc, mouseX, mouseY);
            default -> {}
        }
    }
}