package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber; // Исправленный импорт
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor; // Корректный сетевой дистрибьютор
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

// Исправлено: убран "Mod.", а тип шины изменен на GAME (бывший FORGE)
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.GAME)
public class BlockBreakStaminaHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Исправлено: новый синтаксис отправки пакета конкретному игроку на клиент в NeoForge 1.21.1
        PacketDistributor.sendToPlayer(player, new DrainStaminaPacket(1.0f));
    }
}
