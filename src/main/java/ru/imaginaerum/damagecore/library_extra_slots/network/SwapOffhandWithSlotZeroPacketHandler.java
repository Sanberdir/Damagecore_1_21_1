package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

public final class SwapOffhandWithSlotZeroPacketHandler {

    private SwapOffhandWithSlotZeroPacketHandler() {}

    public static void handle(SwapOffhandWithSlotZeroPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.getInventory() instanceof IExtraSlotsInventory extra)) return;

            ItemStackHandler handler = extra.damagecore$getExtraSlots();
            if (handler == null) return;

            // Читаем/пишем реальный ванильный оффхенд напрямую через список Inventory#offhand,
            // в обход CombatModeHandMixin#getItemBySlot/setItemSlot — эти методы подменяют
            // результат в зависимости от combatMode и иначе ломают своп (теряют предмет
            // вне боя, зацикливаются на слоте 0 в бою).
            ItemStack offhandItem = player.getInventory().offhand.get(0).copy();
            ItemStack slotZeroItem = handler.getStackInSlot(0).copy();

            handler.setStackInSlot(0, offhandItem);
            player.getInventory().offhand.set(0, slotZeroItem);

            player.containerMenu.broadcastChanges();
        });
    }
}