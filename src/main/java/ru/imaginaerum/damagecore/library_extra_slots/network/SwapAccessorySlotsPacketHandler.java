package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SwapAccessorySlotsPacketHandler {

    private SwapAccessorySlotsPacketHandler() {}

    public static void handle(SwapAccessorySlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) return;

            net.neoforged.neoforge.items.ItemStackHandler handler =
                    ((ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory) player.getInventory()).damagecore$getExtraSlots();

            ItemStack offhandStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).copy();
            ItemStack customSlot0  = handler.getStackInSlot(0).copy();
            ItemStack customSlot1 = handler.getStackInSlot(1).copy();
            ItemStack customSlot2 = handler.getStackInSlot(2).copy();

            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, customSlot0);
            handler.setStackInSlot(0, offhandStack);
            handler.setStackInSlot(1, customSlot2);
            handler.setStackInSlot(2, customSlot1);

            // Принудительно заставляем ванильный контейнер обновить стейты на клиенте
            player.containerMenu.broadcastChanges();
        });
    }
}
