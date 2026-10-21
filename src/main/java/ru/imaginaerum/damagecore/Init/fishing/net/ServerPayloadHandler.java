package ru.imaginaerum.damagecore.Init.fishing.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Init.fishing.fight.FightManager;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleInput(FightInputPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                FightManager.onInput(player, payload.dx(), payload.dy());
            }
        });
    }
}
