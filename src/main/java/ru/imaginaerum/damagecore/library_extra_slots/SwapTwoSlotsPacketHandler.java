package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

public final class SwapTwoSlotsPacketHandler {

    private SwapTwoSlotsPacketHandler() {}

    public static void handle(SwapTwoSlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (player.getInventory() instanceof IExtraSlotsInventory extra) {
                ItemStackHandler handler = extra.damagecore$getExtraSlots();
                if (handler != null) {
                    // Индексы 1 и 2 соответствуют Слоту 2 и Слоту 3 в ItemStackHandler(3)
                    ItemStack slot2Item = handler.getStackInSlot(1);
                    ItemStack slot3Item = handler.getStackInSlot(2);

                    // Меняем предметы местами
                    handler.setStackInSlot(1, slot3Item);
                    handler.setStackInSlot(2, slot2Item);

                    // Принудительно обновляем контейнер, чтобы подмена мгновенно отобразилась в HUD и GUI
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
