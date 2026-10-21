package ru.imaginaerum.damagecore.Init.fishing.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FightPosPayload(int hookId, int visualId,
                              double hx, double hy, double hz,
                              double vx, double vy, double vz,
                              float yaw) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FightPosPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "fight_pos"));

    public static final StreamCodec<ByteBuf, FightPosPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.hookId);
                buf.writeInt(p.visualId);
                buf.writeDouble(p.hx);
                buf.writeDouble(p.hy);
                buf.writeDouble(p.hz);
                buf.writeDouble(p.vx);
                buf.writeDouble(p.vy);
                buf.writeDouble(p.vz);
                buf.writeFloat(p.yaw);
            },
            buf -> new FightPosPayload(buf.readInt(), buf.readInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}