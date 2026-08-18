package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AccessorySlot extends SlotItemHandler {

    private final Player owner;

    public AccessorySlot(ModAttachments.ExtraSlotsHandler handler,
                         int index, int x, int y, Player owner) {
        super(handler, index, x, y);
        this.owner = owner;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (owner instanceof ServerPlayer serverPlayer) {
            // Вручную шлем клиенту актуальные данные слотов после клика
            ModAttachments.ExtraSlotsHandler handler = serverPlayer.getData(ModAttachments.EXTRA_SLOTS);
            CompoundTag tag = handler.serializeNBT(serverPlayer.registryAccess());
            ru.imaginaerum.damagecore.api.ModNetwork.sendToClient(new ru.imaginaerum.damagecore.library_extra_slots.network.SyncAccessorySlotsPacket(tag), serverPlayer);
        }
    }

}