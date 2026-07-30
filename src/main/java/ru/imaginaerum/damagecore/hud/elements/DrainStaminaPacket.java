package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.hud.DrainStaminaClientProxy;

public class DrainStaminaPacket implements CustomPacketPayload {

    // Обязательный идентификатор типа пакета для 1.21.1
    public static final Type<DrainStaminaPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "drain_stamina"));

    // Кодек для чтения и записи данных (заменяет старые encode/decode)
    public static final StreamCodec<RegistryFriendlyByteBuf, DrainStaminaPacket> STREAM_CODEC = StreamCodec.of(
            DrainStaminaPacket::encode,
            DrainStaminaPacket::decode
    );

    private final float amount;

    public DrainStaminaPacket(float amount) {
        this.amount = amount;
    }

    private static void encode(RegistryFriendlyByteBuf buf, DrainStaminaPacket packet) {
        buf.writeFloat(packet.amount);
    }

    private static DrainStaminaPacket decode(RegistryFriendlyByteBuf buf) {
        return new DrainStaminaPacket(buf.readFloat());
    }

    // Метод обработки пакета с использованием IPayloadContext
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Безопасная проверка: выполняем код только если пакет пришел на КЛИЕНТ
            if (context.flow().getReceptionSide().isClient()) {
                DrainStaminaClientProxy.drain(this.amount);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
