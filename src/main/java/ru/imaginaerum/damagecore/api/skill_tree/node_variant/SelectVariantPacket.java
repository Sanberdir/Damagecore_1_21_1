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

import java.util.Map;

public record SelectVariantPacket(int treeId, String nodeId, int variantIndex) implements CustomPacketPayload {

    // 1. Уникальный ID пакета (замените "damagecore" на ваш реальный MODID)
    public static final Type<SelectVariantPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("damagecore", "select_variant")
    );

    // 2. Создание StreamCodec для сериализации/десериализации
    public static final StreamCodec<FriendlyByteBuf, SelectVariantPacket> CODEC = StreamCodec.ofMember(
            SelectVariantPacket::encode,
            SelectVariantPacket::decode
    );

    // Метод чтения из буфера
    private static SelectVariantPacket decode(FriendlyByteBuf buf) {
        return new SelectVariantPacket(
                buf.readInt(),
                buf.readUtf(32767),
                buf.readInt()
        );
    }

    // Метод записи в буфер
    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.treeId);
        buf.writeUtf(this.nodeId);
        buf.writeInt(this.variantIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Обработчик пакета (Вызывается на стороне сервера)
    public static void handle(final SelectVariantPacket pkt, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // В NeoForge 1.21.1 игрок-отправитель извлекается через context.player()
            if (!(context.player() instanceof ServerPlayer player)) return;

            SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, pkt.nodeId());
            if (node != null && pkt.variantIndex() >= 0 && pkt.variantIndex() < node.options.size()) {

                node.applyVariant(pkt.variantIndex());
                SkillTreeServerHandler.saveNodeVariant(player, node, pkt.treeId());

                // Синхронизируем клиента с использованием нового статического PacketDistributor
                PacketDistributor.sendToPlayer(player, new SyncNodeVariantsPacket(
                        pkt.treeId(),
                        Map.of(node.id, node.selectedOption)
                ));
            }
        });
    }
}
