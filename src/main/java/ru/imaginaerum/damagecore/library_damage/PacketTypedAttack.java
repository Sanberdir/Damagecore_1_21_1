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

import java.util.ArrayList;
import java.util.List;

public record PacketTypedAttack(
        int targetId,
        List<DamageType> attackTypes,
        double damageMultiplier
) implements CustomPacketPayload {

    public static final Type<PacketTypedAttack> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "typed_attack"));

    public static final StreamCodec<FriendlyByteBuf, PacketTypedAttack> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.targetId());
                buf.writeVarInt(packet.attackTypes().size());
                for (DamageType type : packet.attackTypes()) {
                    buf.writeEnum(type);
                }
                buf.writeDouble(packet.damageMultiplier());
            },
            buf -> {
                int targetId = buf.readVarInt();
                int size = buf.readVarInt();
                List<DamageType> types = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    types.add(buf.readEnum(DamageType.class));
                }
                double mult = buf.readDouble();
                return new PacketTypedAttack(targetId, types, mult);
            }
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
            double baseDamage = 0.0;
            for (DamageType type : payload.attackTypes()) {
                baseDamage += weapon.damagecore$getDamageMap().getOrDefault(type, 0.0);
            }
            if (baseDamage <= 0) return;

            // Применяем множитель из анимации
            double damage = baseDamage * payload.damageMultiplier();

            if (damage <= 0) return;
            var registry = sender.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE);

            Holder.Reference<net.minecraft.world.damagesource.DamageType> typeHolder = registry.getHolderOrThrow(ModDamageTypes.TYPED_ATTACK);

            TypedDamageSource source = new TypedDamageSource(typeHolder, payload.attackTypes(), sender);

            boolean applied = living.hurt(source, (float) damage);
            if (!applied) {
                System.out.println("[TypedAttack] hurt() returned false for "
                        + living.getName().getString() + " dmg=" + damage);
            }
        });
    }
}