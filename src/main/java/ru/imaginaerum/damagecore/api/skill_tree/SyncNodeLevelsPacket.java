package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.Map;

public record SyncNodeLevelsPacket(int treeId, Map<String, Integer> levels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNodeLevelsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sync_node_levels"));

    public static final StreamCodec<FriendlyByteBuf, SyncNodeLevelsPacket> CODEC = StreamCodec.of(
            SyncNodeLevelsPacket::encode,
            SyncNodeLevelsPacket::decode
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, SyncNodeLevelsPacket packet) {
        buf.writeVarInt(packet.treeId);
        buf.writeMap(packet.levels, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    private static SyncNodeLevelsPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readVarInt();
        Map<String, Integer> levels = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt);
        return new SyncNodeLevelsPacket(treeId, levels);
    }

    public static void handle(final SyncNodeLevelsPacket packet, final IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                SkillTreeClientSync
                        .applyNodeLevels(packet.treeId(), packet.levels());
            });
        }
    }
}
