package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HundredArmedSyncPacket(boolean hasSkill) implements CustomPacketPayload {

    // 1. Уникальный ID пакета (замените "damagecore" на ваш реальный MODID)
    public static final Type<HundredArmedSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("damagecore", "hundred_armed_sync")
    );

    // 2. Создание StreamCodec для сериализации/десериализации вместо старых encode/decode
    public static final StreamCodec<FriendlyByteBuf, HundredArmedSyncPacket> CODEC = StreamCodec.ofMember(
            HundredArmedSyncPacket::encode,
            HundredArmedSyncPacket::decode
    );

    // Метод чтения из буфера
    private static HundredArmedSyncPacket decode(FriendlyByteBuf buf) {
        return new HundredArmedSyncPacket(buf.readBoolean());
    }

    // Метод записи в буфер
    private void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.hasSkill);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Обработчик пакета (Вызывается на стороне клиента)
    public static void handle(final HundredArmedSyncPacket packet, final IPayloadContext context) {
        // execute() гарантирует выполнение в основном потоке Майнкрафта
        context.enqueueWork(() -> {
            // Безопасно проверяем сторону через контекст NeoForge перед вызовом прокси
            if (context.flow().isClientbound()) {
                HundredArmedClientProxy.apply(packet.hasSkill());
            }
        });
    }
}
