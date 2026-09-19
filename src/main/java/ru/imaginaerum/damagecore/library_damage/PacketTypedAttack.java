package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketTypedAttack(
        int targetId,
        DamageType attackType,
        double damageMultiplier
) implements CustomPacketPayload {

    public static final Type<PacketTypedAttack> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "typed_attack"));

    public static final StreamCodec<FriendlyByteBuf, PacketTypedAttack> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.targetId());
                buf.writeEnum(packet.attackType());
                buf.writeDouble(packet.damageMultiplier());
            },
            buf -> new PacketTypedAttack(
                    buf.readVarInt(),
                    buf.readEnum(DamageType.class),
                    buf.readDouble()
            )
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

            // Базовый урон оружия для данного типа
            double baseDamage = weapon.damagecore$getDamageMap()
                    .getOrDefault(payload.attackType(), 0.0);

            if (baseDamage <= 0) return;

            // Применяем множитель из анимации
            double damage = baseDamage * payload.damageMultiplier();

            if (damage <= 0) return;
            var registry = sender.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE);

            Holder.Reference<net.minecraft.world.damagesource.DamageType> typeHolder = registry.getHolderOrThrow(ModDamageTypes.TYPED_ATTACK);

            TypedDamageSource source = new TypedDamageSource(typeHolder, payload.attackType(), sender);

            boolean applied = living.hurt(source, (float) damage);
            if (!applied) {
                System.out.println("[TypedAttack] hurt() returned false for "
                        + living.getName().getString() + " dmg=" + damage);
            }
        });
    }
}