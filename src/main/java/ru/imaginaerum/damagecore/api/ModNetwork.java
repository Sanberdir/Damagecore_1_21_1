package ru.imaginaerum.damagecore.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow.ChainLightningPacket;
import ru.imaginaerum.damagecore.hud.elements.DrainStaminaPacket;
import ru.imaginaerum.damagecore.hud.elements.NormalAttackPacket;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;

@EventBusSubscriber(modid = "damagecore")
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                ChainLightningPacket.TYPE,
                ChainLightningPacket.CODEC,
                ChainLightningPacket::handleClient
        );

        registrar.playToServer(
                ThirstDamagePacket.TYPE,
                ThirstDamagePacket.STREAM_CODEC,
                ThirstDamagePacket::handle
        );

        registrar.playToClient(
                DrainStaminaPacket.TYPE,
                DrainStaminaPacket.STREAM_CODEC,
                DrainStaminaPacket::handle
        );

        registrar.playBidirectional(
                NormalAttackPacket.TYPE,
                NormalAttackPacket.STREAM_CODEC,
                NormalAttackPacket::handle
        );
    }

    // ─── Удобные методы отправки, чтобы не разбрасывать PacketDistributor по всему проекту ───

    public static void sendToClient(DrainStaminaPacket msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    public static void sendToServer(ThirstDamagePacket msg) {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendToServer(NormalAttackPacket msg) {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendToClientOrServer(NormalAttackPacket msg, ServerPlayer player) {
        // playBidirectional: если нужно отправить конкретному игроку с сервера
        PacketDistributor.sendToPlayer(player, msg);
    }
}