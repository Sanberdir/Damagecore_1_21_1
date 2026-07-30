package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NormalAttackPacket implements CustomPacketPayload {

    // Обязательный идентификатор типа пакета для 1.21.1
    public static final Type<NormalAttackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "normal_attack"));

    // Кодек для сериализации и десериализации (заменяет старые encode/decode)
    public static final StreamCodec<RegistryFriendlyByteBuf, NormalAttackPacket> STREAM_CODEC = StreamCodec.of(
            NormalAttackPacket::encode,
            NormalAttackPacket::decode
    );

    public NormalAttackPacket() {}

    private static void encode(RegistryFriendlyByteBuf buf, NormalAttackPacket packet) {
        // Пакет пустой, ничего не записываем в буфер
    }

    private static NormalAttackPacket decode(RegistryFriendlyByteBuf buf) {
        return new NormalAttackPacket();
    }

    // Метод обработки пакета с новым контекстом IPayloadContext
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Проверяем, что пакет пришел на сервер и отправитель — игрок
            if (context.player() instanceof ServerPlayer attacker) {

                // Дренаж стамины — шлём обратно клиенту через context.reply()
                context.reply(new DrainStaminaPacket(4.0f));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
