package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SwapTwoSlotsPacket() implements CustomPacketPayload {

    public static final Type<SwapTwoSlotsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "swap_two_slots"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapTwoSlotsPacket> STREAM_CODEC =
            StreamCodec.unit(new SwapTwoSlotsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
