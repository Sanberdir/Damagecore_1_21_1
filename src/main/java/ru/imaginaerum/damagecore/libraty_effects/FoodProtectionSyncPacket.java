package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public record FoodProtectionSyncPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<FoodProtectionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "food_protection_sync"));

    public static final StreamCodec<FriendlyByteBuf, FoodProtectionSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeNbt(pkt.data()),
            buf -> new FoodProtectionSyncPacket(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final FoodProtectionSyncPacket pkt, final IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            FoodProtectionClientProxy.apply(pkt.data());
        }
    }
}