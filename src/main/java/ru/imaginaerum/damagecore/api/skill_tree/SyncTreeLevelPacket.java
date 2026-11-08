package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.HashMap;
import java.util.Map;

public record SyncTreeLevelPacket(int treeId, int level) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncTreeLevelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    Damagecore_1_21_1_neo.MODID, "sync_tree_level"));

    public static final StreamCodec<FriendlyByteBuf, SyncTreeLevelPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.treeId); buf.writeVarInt(p.level); },
            buf -> new SyncTreeLevelPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final SyncTreeLevelPacket packet, final IPayloadContext ctx) {
        System.out.println("[Client] tree level " + packet.treeId() + " = " + packet.level());
        if (!ctx.flow().isClientbound()) return;
        ctx.enqueueWork(() -> {
            DamageBookRenderer.setLevel(packet.treeId(), packet.level());

            Map<String, Integer> cached = SkillTreeClientSync.getLevelCache(packet.treeId());
            SkillTreeClientSync.applyNodeLevels(packet.treeId(),
                    cached != null ? cached : new HashMap<>());
        });
    }
}