package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@Mixin(Player.class)
public abstract class PlayerStorageMixin {

    // Записываем данные наших слотов на жесткий диск
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void damagecore$writeCustomSlotsToDisk(CompoundTag compoundTag, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.getInventory() instanceof IExtraSlotsInventory extraSlotsInventory) {
            ItemStackHandler handler = extraSlotsInventory.damagecore$getExtraSlots();
            if (handler != null) {
                // Сохраняем в чистый изолированный тег, который ваниль не будет трогать
                compoundTag.put("DamageCoreSlots", handler.serializeNBT(self.level().registryAccess()));
            }
        }
    }

    // Читаем данные наших слотов при загрузке мира
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void damagecore$readCustomSlotsFromDisk(CompoundTag compoundTag, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (compoundTag.contains("DamageCoreSlots")) {
            if (self.getInventory() instanceof IExtraSlotsInventory extraSlotsInventory) {
                ItemStackHandler handler = extraSlotsInventory.damagecore$getExtraSlots();
                if (handler != null) {
                    CompoundTag slotsData = compoundTag.getCompound("DamageCoreSlots");
                    handler.deserializeNBT(self.level().registryAccess(), slotsData);
                }
            }
        }
    }
}
