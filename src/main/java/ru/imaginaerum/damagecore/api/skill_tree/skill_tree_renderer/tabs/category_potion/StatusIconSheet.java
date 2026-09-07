// StatusIconSheet.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

/** Общий спрайт-лист, из которого вырезаются статичные иконки категорий (еда/зелья/моб). */
final class StatusIconSheet {
    private StatusIconSheet() {}

    static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "textures/gui/icons/status_effect_icons.png");

    static final int TEX_W = 256;
    static final int TEX_H = 256;

    static void blit(GuiGraphics gui, int x, int y, int u, int v, int region) {
        gui.blit(SHEET,
                x, y, EffectIconLayout.ICON_SIZE, EffectIconLayout.ICON_SIZE,
                u, v, region, region, TEX_W, TEX_H);
    }
}