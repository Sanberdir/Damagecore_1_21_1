package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeAccessorySlotFixMixin {

    @Shadow public ServerPlayer player;

    // Индексы наших слотов внутри InventoryMenu: сразу после стандартных 0..45
    private static final int ACCESSORY_SLOT_START = 46;
    private static final int ACCESSORY_SLOT_COUNT = 3;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void damagecore$fixAccessorySlotWrite(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int slotNum = packet.slotNum();
        if (slotNum < ACCESSORY_SLOT_START || slotNum >= ACCESSORY_SLOT_START + ACCESSORY_SLOT_COUNT) {
            return; // не наш слот — пусть ваниль обрабатывает как обычно
        }
        if (!this.player.getAbilities().instabuild) return; // только для реального креатива

        if (!(this.player.getInventory() instanceof IExtraSlotsInventory extra)) return;
        ItemStackHandler handler = extra.damagecore$getExtraSlots();
        if (handler == null) return;

        int handlerIndex = slotNum - ACCESSORY_SLOT_START;
        handler.setStackInSlot(handlerIndex, packet.itemStack().copy());

        this.player.inventoryMenu.getSlot(slotNum).setChanged();
        this.player.inventoryMenu.broadcastFullState();

        // Не даём ванильному коду с хардкод-диапазоном 0..45 обработать этот пакет
        ci.cancel();
    }
}