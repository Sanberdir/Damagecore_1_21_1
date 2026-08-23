package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@Mixin(Player.class)
public abstract class CombatModeHandMixin implements ICombatModeEntity {

    @Unique
    private boolean damagecore$combatMode = false;

    @Unique
    @Override
    public boolean damagecore$isCombatMode() {
        return this.damagecore$combatMode;
    }

    @Unique
    @Override
    public void damagecore$setCombatMode(boolean value) {
        this.damagecore$combatMode = value;
    }

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void damagecore$overrideHandSlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        // ЛОГИКА ДЛЯ ЛЕВОЙ РУКИ (OFFHAND)
        if (slot == EquipmentSlot.OFFHAND) {
            if (!this.damagecore$combatMode) {
                cir.setReturnValue(ItemStack.EMPTY); // Скрываем вне боя
            }
            return; // В бою возвращает стандартный предмет оффхенда
        }

        // ЛОГИКА ДЛЯ ПРАВОЙ РУКИ (MAINHAND) — оставляем твою подмену на Слот 2
        if (slot == EquipmentSlot.MAINHAND) {
            if (!this.damagecore$combatMode) return;

            Player self = (Player) (Object) this;
            if (self.getInventory() instanceof IExtraSlotsInventory extra) {
                ItemStackHandler handler = extra.damagecore$getExtraSlots();
                if (handler != null) {
                    cir.setReturnValue(handler.getStackInSlot(2));
                }
            }
        }
    }


    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void damagecore$redirectHandSlotWrite(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (!this.damagecore$combatMode) return; // вне режима — ванильная запись
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;

        Player self = (Player) (Object) this;
        if (!(self.getInventory() instanceof IExtraSlotsInventory extra)) return;
        ItemStackHandler handler = extra.damagecore$getExtraSlots();
        if (handler == null) return;

        int index = (slot == EquipmentSlot.MAINHAND) ? 2 : 0;
        handler.setStackInSlot(index, stack);
        ci.cancel();
    }
}