package ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainLightningPacket(Vec3 start, Vec3 end) implements CustomPacketPayload {

    public static final Type<ChainLightningPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "chain_lightning"));

    public static final StreamCodec<FriendlyByteBuf, ChainLightningPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeDouble(packet.start.x); buf.writeDouble(packet.start.y); buf.writeDouble(packet.start.z);
                buf.writeDouble(packet.end.x); buf.writeDouble(packet.end.y); buf.writeDouble(packet.end.z);
            },
            buf -> new ChainLightningPacket(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(final ChainLightningPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // ВМЕСТО ЧАСТИЦ: Отправляем две точки напрямую в 3D-рендерер линий!
            ClientLightningRenderer.addChainSegment(payload.start(), payload.end());
        });
    }
}
