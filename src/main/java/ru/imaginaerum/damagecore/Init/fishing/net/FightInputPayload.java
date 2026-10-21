package ru.imaginaerum.damagecore.Init.fishing.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

/** Client -> server: mouse movement (raw pixels) accumulated since the last client tick. */
public record FightInputPayload(float dx, float dy) implements CustomPacketPayload {

    public static final Type<FightInputPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fight_input"));

    public static final StreamCodec<ByteBuf, FightInputPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FightInputPayload::dx,
            ByteBufCodecs.FLOAT, FightInputPayload::dy,
            FightInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
