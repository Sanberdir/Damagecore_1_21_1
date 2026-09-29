package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.Render;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkillTreeMouseHandler {

    @SubscribeEvent
    public static void onGuiMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!(screen instanceof ISkillTreeAccessor accessor)) return;
        if (!accessor.damagecore$isSkillTreeVisible()) return; // панель закрыта — ваниль

        double mx = event.getMouseX();
        double my = event.getMouseY();
        double delta = event.getScrollDeltaY();

        // область дерева навыков
        int treeX1 = Render.currentPanelScreenX + Render.PANEL_DRAW_OFFSET_X_IN_PANEL;
        int treeY1 = Render.currentPanelScreenY + Render.PANEL_DRAW_OFFSET_Y_IN_PANEL;
        boolean overTree = mx >= treeX1 && mx <= treeX1 + Render.AREA_WIDTH
                && my >= treeY1 && my <= treeY1 + Render.AREA_HEIGHT;

        if (overTree) {
            // зум дерева только когда курсор над деревом
            boolean used = SkillTreeRenderer.mouseScrolled(
                    (int) mx, (int) my, delta,
                    Render.currentPanelScreenX, Render.currentPanelScreenY);
            if (used) event.setCanceled(true);
        } else if (mx < treeX1) {
            // слева от дерева — панель статов, листаем список
            accessor.damagecore$scrollList(delta);
            event.setCanceled(true);
        }
    }
}