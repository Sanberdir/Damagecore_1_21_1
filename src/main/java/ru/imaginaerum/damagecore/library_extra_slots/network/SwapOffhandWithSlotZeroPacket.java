package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SwapOffhandWithSlotZeroPacket() implements CustomPacketPayload {

    public static final Type<SwapOffhandWithSlotZeroPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "swap_offhand_slot_zero"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapOffhandWithSlotZeroPacket> STREAM_CODEC =
            StreamCodec.unit(new SwapOffhandWithSlotZeroPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
