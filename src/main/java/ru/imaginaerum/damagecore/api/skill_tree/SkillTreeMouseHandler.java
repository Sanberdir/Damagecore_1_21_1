package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo; // Импортируйте ваш главный класс мода

// ИСПРАВЛЕНО: Рекомендуется использовать константу MODID вместо хардкода строки
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkillTreeMouseHandler {

    @SubscribeEvent
    public static void onGuiMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        // ПРИМЕЧАНИЕ: Использование .Pre вместо базового события позволяет отменить
        // прокрутку ДО того, как её обработает ванильный инвентарь (например, прокрутит список рецептов)

        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();

        // Получаем актуальные координаты панели из DamageBookRenderer
        int panelScreenX = 100; // TODO: получить реальные координаты
        int panelScreenY = 100;

        // ИСПРАВЛЕНО: Вместо event.getScrollDelta() берем event.getScrollDeltaY() для вертикального скролла
        boolean used = SkillTreeRenderer.mouseScrolled(
                (int) event.getMouseX(),
                (int) event.getMouseY(),
                event.getScrollDeltaY(),
                panelScreenX,
                panelScreenY
        );

        if (used) {
            event.setCanceled(true);

            // ДОБАВИТЬ: принудительное обновление рендера
            // SkillTreeRenderer.forceRecalculate();
        }
    }
}
