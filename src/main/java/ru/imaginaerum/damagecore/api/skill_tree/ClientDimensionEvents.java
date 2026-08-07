package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo; // Импортируйте ваш главный класс мода

// ИСПРАВЛЕНО: bus теперь Bus.GAME вместо Bus.FORGE. Также используем константу MODID.
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientDimensionEvents {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Проверяем, что событие обрабатывается именно на локальном игроке (клиенте)
        if (!event.getEntity().level().isClientSide()) return;

        // Сбрасываем всё состояние skill tree
        SkillTreeRenderer.clearAllCaches(); // очищает деревья, кэши и активное дерево
        ClientSyncState.syncRequested = false;

        // Запрашиваем полную синхронизацию у сервера
        if (Minecraft.getInstance().player != null) {
            // ИСПРАВЛЕНО: Используем новый сетевой API NeoForge 1.21.1
            PacketDistributor.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }
    }
}
