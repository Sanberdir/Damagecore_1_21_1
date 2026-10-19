package ru.imaginaerum.damagecore.key_bind;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AltPlaceNetwork {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                AltPlacePayload.TYPE,
                AltPlacePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(
                        () -> AltPlaceCommon.set(ctx.player().getUUID(), payload.down())));
    }
}