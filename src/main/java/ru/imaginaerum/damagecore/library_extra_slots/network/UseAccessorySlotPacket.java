package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UseAccessorySlotPacket() implements CustomPacketPayload {

    public static final Type<UseAccessorySlotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "use_accessory_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseAccessorySlotPacket> STREAM_CODEC =
            StreamCodec.unit(new UseAccessorySlotPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}