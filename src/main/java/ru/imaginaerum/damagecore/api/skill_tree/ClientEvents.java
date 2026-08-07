package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

// ИСПРАВЛЕНО: bus = Bus.GAME для игровых сетевых событий
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Полная очистка состояния (деревья + кэш)
        SkillTreeRenderer.clearAllCaches();
        ClientSyncState.syncRequested = false;

        // Теперь безопасно запросить новый синк
        if (Minecraft.getInstance().player != null) {
            // ИСПРАВЛЕНО: Новый сетевой синтаксис NeoForge 1.21.1
            PacketDistributor.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SkillTreeRenderer.clearAllCaches();
        ClientSyncState.syncRequested = false;
    }
}
