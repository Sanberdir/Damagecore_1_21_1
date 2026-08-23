package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.library_extra_slots.network.SwapAccessorySlotsPacket;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public final class ClientAccessoryKeyHandler {

    private ClientAccessoryKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Только клавиша F. Все остальные комбинации теперь перехвачены намертво через Mixins!
        while (ModKeyMappings.USE_ACCESSORY_SLOT.consumeClick()) {
            PacketDistributor.sendToServer(new SwapAccessorySlotsPacket());
        }
    }
}
