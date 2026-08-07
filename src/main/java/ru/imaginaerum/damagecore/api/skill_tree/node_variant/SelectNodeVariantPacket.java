package ru.imaginaerum.damagecore.api.skill_tree.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerRegistry;

import java.util.HashMap;
import java.util.Map;

public record SelectNodeVariantPacket(int treeId, String nodeId, int variant) implements CustomPacketPayload {

    // 1. Уникальный ID пакета (замените "damagecore" на ваш реальный MODID)
    public static final Type<SelectNodeVariantPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("damagecore", "select_node_variant")
    );

    // 2. Создание StreamCodec для сериализации/десериализации взамен encode/decode
    public static final StreamCodec<FriendlyByteBuf, SelectNodeVariantPacket> CODEC = StreamCodec.ofMember(
            SelectNodeVariantPacket::encode,
            SelectNodeVariantPacket::decode
    );

    // Метод чтения из буфера
    private static SelectNodeVariantPacket decode(FriendlyByteBuf buf) {
        return new SelectNodeVariantPacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt()
        );
    }

    // Метод записи в буфер
    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.treeId);
        buf.writeUtf(this.nodeId);
        buf.writeInt(this.variant);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Обработчик пакета (Вызывается на стороне сервера)
    public static void handle(final SelectNodeVariantPacket pkt, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // В 1.21.1 игрок-отправитель извлекается из контекста
            if (!(context.player() instanceof ServerPlayer player)) return;

            Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(pkt.treeId());
            if (nodes == null || nodes.isEmpty()) return;

            SkillTreeNode node = nodes.get(pkt.nodeId());
            if (node == null) return;

            node.applyVariant(pkt.variant());
            SkillTreeServerHandler.saveNodeVariant(player, node, pkt.treeId());

            Map<String, Integer> sync = new HashMap<>();
            sync.put(pkt.nodeId(), pkt.variant());

            // Новый синтаксис отправки пакета с сервера обратно игроку на клиент
            PacketDistributor.sendToPlayer(player, new SyncNodeVariantsPacket(pkt.treeId(), sync));

            System.out.println("[Server] Variant selected: " + pkt.nodeId() + " = " + pkt.variant());
        });
    }
}
