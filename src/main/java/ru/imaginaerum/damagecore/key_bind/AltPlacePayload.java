package ru.imaginaerum.damagecore.key_bind;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public record AltPlacePayload(boolean down) implements CustomPacketPayload {
    public static final Type<AltPlacePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "alt_place"));

    public static final StreamCodec<ByteBuf, AltPlacePayload> CODEC =
            ByteBufCodecs.BOOL.map(AltPlacePayload::new, AltPlacePayload::down);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}