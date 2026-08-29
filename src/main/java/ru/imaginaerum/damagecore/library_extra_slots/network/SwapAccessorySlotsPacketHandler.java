package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;

public final class SwapAccessorySlotsPacketHandler {

    private SwapAccessorySlotsPacketHandler() {}

    public static void handle(SwapAccessorySlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ICombatModeEntity combatEntity = (ICombatModeEntity) player;
            // Инвертируем текущее состояние: если был false, станет true
            boolean newState = !combatEntity.damagecore$isCombatMode();
            combatEntity.damagecore$setCombatMode(newState);
            // Отправляем пакет синхронизации ВСЕМ, кто видит игрока, и самому игроку (на клиент)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player, new SyncCombatModePacket(player.getId(), newState));

            // Принудительно обновляем контейнер, чтобы подмена рук применилась в GUI
            player.containerMenu.broadcastChanges();
        });
    }
}
