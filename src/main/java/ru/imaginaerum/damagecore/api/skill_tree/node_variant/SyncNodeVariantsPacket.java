package ru.imaginaerum.damagecore.api.skill_tree.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeClientSync;

import java.util.Map;

public record SyncNodeVariantsPacket(int treeId, Map<String, Integer> variants) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNodeVariantsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sync_node_variants"));

    public static final StreamCodec<FriendlyByteBuf, SyncNodeVariantsPacket> CODEC = StreamCodec.of(
            SyncNodeVariantsPacket::encode,
            SyncNodeVariantsPacket::decode
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, SyncNodeVariantsPacket packet) {
        buf.writeVarInt(packet.treeId);
        buf.writeMap(packet.variants, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    private static SyncNodeVariantsPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readVarInt();
        Map<String, Integer> variants = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt);
        return new SyncNodeVariantsPacket(treeId, variants);
    }

    public static void handle(final SyncNodeVariantsPacket packet, final IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            ctx.enqueueWork(() -> {
                SkillTreeClientSync
                        .applyVariants(packet.treeId(), packet.variants());
            });
        }
    }
}
