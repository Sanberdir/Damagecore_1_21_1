package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_extra_slots.network.SyncAccessorySlotsPacket;

@EventBusSubscriber(modid = "damagecore")
public final class ExtraSlotsJoinHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Берем хендлер. Благодаря изменениям в миксине, серверный игрок к этому моменту
            // гарантированно имеет тот же хендлер, в который считались данные с диска.
            ModAttachments.ExtraSlotsHandler handler = serverPlayer.getData(ModAttachments.EXTRA_SLOTS);

            CompoundTag tag = handler.serializeNBT(serverPlayer.registryAccess());
            ModNetwork.sendToClient(new SyncAccessorySlotsPacket(tag), serverPlayer);
        }
    }
}
