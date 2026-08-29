package ru.imaginaerum.damagecore.library_weapon_types;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public record SetPendingAttackDamageTypePayload(String damageTypeName) implements CustomPacketPayload {

    public static final Type<SetPendingAttackDamageTypePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "pending_attack_damage_type"));

    public static final StreamCodec<ByteBuf, SetPendingAttackDamageTypePayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(SetPendingAttackDamageTypePayload::new, SetPendingAttackDamageTypePayload::damageTypeName);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public DamageType toDamageType() {
        if (damageTypeName == null || damageTypeName.isEmpty()) return null;
        try {
            return DamageType.valueOf(damageTypeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}