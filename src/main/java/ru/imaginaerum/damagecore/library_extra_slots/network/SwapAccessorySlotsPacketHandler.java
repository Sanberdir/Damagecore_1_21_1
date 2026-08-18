package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_extra_slots.ModAttachments;

public final class SwapAccessorySlotsPacketHandler {

    private SwapAccessorySlotsPacketHandler() {}

    public static void handle(SwapAccessorySlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ModAttachments.EXTRA_SLOTS.isBound()) return;
            if (!player.hasData(ModAttachments.EXTRA_SLOTS)) return;

            ModAttachments.ExtraSlotsHandler handler =
                    (ModAttachments.ExtraSlotsHandler) player.getData(ModAttachments.EXTRA_SLOTS);

            // --- ПЕРВАЯ ПАРА ОБМЕНА: Слот щита <-> Кастомный слот 1 (индекс 0) ---
            ItemStack offhandStack = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();
            ItemStack customSlot0  = handler.getStackInSlot(0).copy();

            player.setItemSlot(EquipmentSlot.OFFHAND, customSlot0); // В щит кладём вещь из 1-го кастомного
            handler.setStackInSlot(0, offhandStack);                 // В 1-й кастомный кладём вещь из щита


            // --- ВТОРАЯ ПАРА ОБМЕНА: Кастомный слот 2 (индекс 1) <-> Кастомный слот 3 (индекс 2) ---
            ItemStack customSlot1 = handler.getStackInSlot(1).copy();
            ItemStack customSlot2 = handler.getStackInSlot(2).copy();

            handler.setStackInSlot(1, customSlot2); // Во 2-й кастомный кладём вещь из 3-го
            handler.setStackInSlot(2, customSlot1); // В 3-й кастомный кладём вещь из 2-го


            CompoundTag tag = handler.serializeNBT(player.registryAccess());
            ModNetwork.sendToClient(new ru.imaginaerum.damagecore.library_extra_slots.network.SyncAccessorySlotsPacket(tag), player);
        });
    }
}
