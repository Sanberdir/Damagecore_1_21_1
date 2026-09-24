package ru.imaginaerum.damagecore.animation_attack;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public record PacketBreakBlock(BlockPos pos) implements CustomPacketPayload {

    public static final Type<PacketBreakBlock> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "break_block"));

    public static final StreamCodec<ByteBuf, PacketBreakBlock> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PacketBreakBlock::pos,
                    PacketBreakBlock::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}