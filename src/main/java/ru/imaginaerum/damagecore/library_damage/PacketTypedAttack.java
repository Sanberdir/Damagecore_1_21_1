package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketTypedAttack(int targetId, DamageType attackType) implements CustomPacketPayload {

    public static final Type<PacketTypedAttack> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "typed_attack"));

    public static final StreamCodec<FriendlyByteBuf, PacketTypedAttack> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.targetId());
                buf.writeEnum(packet.attackType());
            },
            buf -> new PacketTypedAttack(buf.readVarInt(), buf.readEnum(DamageType.class))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketTypedAttack payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;

            if (!(sender.getMainHandItem().getItem() instanceof IDamageCoreWeapon weapon)) return;

            Entity target = sender.level().getEntity(payload.targetId());
            if (!(target instanceof LivingEntity living)) return;

            double damage = weapon.damagecore$getDamageMap().getOrDefault(payload.attackType(), 0.0);
            if (damage <= 0) return;

            TypedDamageSource source = new TypedDamageSource(
                    sender.level().damageSources().playerAttack(sender).typeHolder(),
                    payload.attackType(),
                    sender
            );

            living.hurt(source, (float) damage);
            sender.resetAttackStrengthTicker();
        });
    }
}