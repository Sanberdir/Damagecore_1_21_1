package ru.imaginaerum.damagecore.hud.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ThirstDamagePacket implements CustomPacketPayload {

    public static final Type<ThirstDamagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "thirst_damage"));

    private static final ThirstDamagePacket INSTANCE = new ThirstDamagePacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, ThirstDamagePacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public ThirstDamagePacket() {}

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            Difficulty difficulty = player.level().getDifficulty();
            float health = player.getHealth();

            boolean canDamage = switch (difficulty) {
                case PEACEFUL -> false;
                case EASY -> health > 10.0f;   // не ниже 5 сердец
                case NORMAL -> health > 1.0f;  // не убивает
                case HARD -> true;             // может убить
            };

            if (canDamage) {
                player.hurt(player.damageSources().starve(), 1.0f);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}