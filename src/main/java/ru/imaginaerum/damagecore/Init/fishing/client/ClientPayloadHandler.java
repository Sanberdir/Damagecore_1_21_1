package ru.imaginaerum.damagecore.Init.fishing.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Init.fishing.net.FightPosPayload;
import ru.imaginaerum.damagecore.Init.fishing.net.FightStatePayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}
    public static void handlePos(FightPosPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FightClient.applyPos(payload));
    }
    public static void handleState(FightStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FightClient.applyState(payload));
    }
}
