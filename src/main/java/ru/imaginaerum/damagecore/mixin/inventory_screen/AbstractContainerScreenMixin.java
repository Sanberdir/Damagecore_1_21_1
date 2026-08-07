package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.api.skill_tree.ISkillTreeAccessor;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    /**
     * Скрывает слоты крафта (0 = результат, 1–4 = сетка 2×2) в инвентаре выживания,
     * но ТОЛЬКО когда открыто дерево прогресса.
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;isActive()Z"
            )
    )
    private boolean damagecore$hideCraftingSlots(Slot slot) {
        if ((Object) this instanceof InventoryScreen inventoryScreen
                && ((ISkillTreeAccessor) inventoryScreen).damagecore$isSkillTreeVisible()
                && slot.index <= 4) {
            return false;
        }
        return slot.isActive();
    }

}