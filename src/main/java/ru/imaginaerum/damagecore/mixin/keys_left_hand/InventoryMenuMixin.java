package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.AccessorySlot;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Unique private static final int DAMAGECORE_SLOT_X = 77;
    @Unique private static final int DAMAGECORE_SLOT_Y_BASE = 62;
    @Unique private static final int DAMAGECORE_SLOT_STEP = 18;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void damagecore$addExtraSlots(Inventory inventory, boolean isCrafting, Player owner, CallbackInfo ci) {
        AbstractContainerMenuInvoker invoker = (AbstractContainerMenuInvoker) (Object) this;

        // Берем хендлер прямо из ванильного инвентаря через интерфейс-утку
        ItemStackHandler handler = ((IExtraSlotsInventory) inventory).damagecore$getExtraSlots();

        for (int i = 0; i < 3; i++) {
            int slotY = DAMAGECORE_SLOT_Y_BASE - DAMAGECORE_SLOT_STEP * (i + 1);
            // Передаем кастомный хендлер инвентаря в AccessorySlot
            invoker.damagecore$addSlot(new AccessorySlot(handler, i, DAMAGECORE_SLOT_X, slotY, owner));
        }
    }
}
