package ru.imaginaerum.damagecore.Init.fishing.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

/** Client -> server: pressed keys. side: -1 = A/left, 1 = D/right. */
public record FightKeysPayload(byte side, boolean tighten, boolean slack) implements CustomPacketPayload {

    public static final Type<FightKeysPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fight_keys"));

    public static final StreamCodec<ByteBuf, FightKeysPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, FightKeysPayload::side,
            ByteBufCodecs.BOOL, FightKeysPayload::tighten,
            ByteBufCodecs.BOOL, FightKeysPayload::slack,
            FightKeysPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}