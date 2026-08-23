package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@EventBusSubscriber(modid = "damagecore")
public final class ExtraSlotsJoinHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // ИСПРАВЛЕНО: Берем хендлер напрямую из расширенного ванильного инвентаря игрока
            ItemStackHandler handler = ((IExtraSlotsInventory) serverPlayer.getInventory()).damagecore$getExtraSlots();

            if (handler != null) {
                CompoundTag tag = handler.serializeNBT(serverPlayer.registryAccess());
                ModNetwork.sendToClient(new SyncAccessorySlotsPacket(tag), serverPlayer);
            }
        }
    }
}
