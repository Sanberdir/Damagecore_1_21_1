package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

public final class DamageBookPositionHelper {
    private DamageBookPositionHelper() {}

    /**
     * Обновляет позицию инвентаря с учётом левой и правой панелей.
     *
     * @param screen          экран инвентаря
     * @param leftTabVisible  видима ли левая панель (Damage Book)
     * @param rightTabVisible видима ли правая панель (skill tree / interface)
     * @param leftTabWidth    ширина левой панели
     * @param rightTabWidth   ширина правой панели (справа от инвентаря)
     */
    public static void updateInventoryPosition(InventoryScreen screen, boolean leftTabVisible, boolean rightTabVisible, int leftTabWidth, int rightTabWidth) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;

        if (screen.getRecipeBookComponent().isVisible()) {
            return;
        }

        int totalWidth = accessor.damagecore$getImageWidth();
        if (leftTabVisible) totalWidth += leftTabWidth;
        if (rightTabVisible) totalWidth += rightTabWidth;

        int newLeftPos = (screen.width - totalWidth) / 2 + 2;

        // Если видна левая панель — сдвинем инвентарь вправо, чтобы слева появился таб.
        // Если видна только правая панель — оставляем инвентарь слева в той же позиции, а дополнительная ширина
        // займёт пространство справа.
        if (leftTabVisible) {
            accessor.setLeftPos(newLeftPos + leftTabWidth);
        } else {
            accessor.setLeftPos(newLeftPos);
        }
    }
}
