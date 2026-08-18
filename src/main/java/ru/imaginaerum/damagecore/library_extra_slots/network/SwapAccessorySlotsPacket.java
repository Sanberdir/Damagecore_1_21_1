package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SwapAccessorySlotsPacket() implements CustomPacketPayload {

    public static final Type<SwapAccessorySlotsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "swap_accessory_slots"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapAccessorySlotsPacket> STREAM_CODEC =
            StreamCodec.unit(new SwapAccessorySlotsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
