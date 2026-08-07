package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LearnNodePacket(int treeId, String nodeId) implements CustomPacketPayload {

    // 1. Уникальный ID пакета (замените "damagecore" на ваш реальный MODID)
    public static final Type<LearnNodePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("damagecore", "learn_node")
    );

    // 2. Создание StreamCodec для сериализации/десериализации взамен encode/decode
    public static final StreamCodec<FriendlyByteBuf, LearnNodePacket> CODEC = StreamCodec.ofMember(
            LearnNodePacket::encode,
            LearnNodePacket::decode
    );

    // Метод чтения из буфера
    private static LearnNodePacket decode(FriendlyByteBuf buf) {
        int tid = buf.readInt();
        String nid = buf.readUtf(32767);
        return new LearnNodePacket(tid, nid);
    }

    // Метод записи в буфер
    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.treeId);
        buf.writeUtf(this.nodeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Обработчик пакета (Вызывается на стороне сервера)
    public static void handle(final LearnNodePacket pkt, final IPayloadContext context) {
        // enqueueWork гарантирует выполнение логики в основном потоке сервера
        context.enqueueWork(() -> {
            // В NeoForge 1.21.1 игрок-отправитель извлекается через context.player()
            if (!(context.player() instanceof ServerPlayer player)) return;

            SkillTreeNode node = SkillTreeServerRegistry.getNode(pkt.treeId(), pkt.nodeId());
            if (node == null) return;

            SkillTreeServerHandler.handleLearnRequest(player, pkt.treeId(), pkt.nodeId());
        });
    }
}
