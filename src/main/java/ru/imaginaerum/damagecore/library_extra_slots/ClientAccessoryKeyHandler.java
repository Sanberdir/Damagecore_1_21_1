package ru.imaginaerum.damagecore.library_extra_slots;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
// ИСПРАВЛЕНО: Импортируем именно пакет обмена
import ru.imaginaerum.damagecore.library_extra_slots.network.SwapAccessorySlotsPacket;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public final class ClientAccessoryKeyHandler {

    private ClientAccessoryKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Слушаем нажатие клавиши F
        while (ModKeyMappings.USE_ACCESSORY_SLOT.consumeClick()) {
            // ИСПРАВЛЕНО: Отправляем новый зарегистрированный пакет на сервер
            PacketDistributor.sendToServer(new SwapAccessorySlotsPacket());
        }
    }
}
