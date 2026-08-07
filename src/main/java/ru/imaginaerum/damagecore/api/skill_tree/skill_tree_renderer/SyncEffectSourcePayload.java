package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public record SyncEffectSourcePayload(MobEffect effect, EntityType<?> sourceType) implements CustomPacketPayload {

    // В 1.21.1 ID пакета регистрируется через тип CustomPacketPayload.Type
    public static final CustomPacketPayload.Type<SyncEffectSourcePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sync_effect_source")
    );

    // Сетевой кодек для чтения и записи данных
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEffectSourcePayload> STREAM_CODEC = StreamCodec.of(
            SyncEffectSourcePayload::encode, SyncEffectSourcePayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, SyncEffectSourcePayload msg) {
        // Получаем Holder для эффекта и пишем его через реестр
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(msg.effect);
        ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT).encode(buf, holder);

        // Пишем EntityType через ID ресурса
        buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(msg.sourceType));
    }

    private static SyncEffectSourcePayload decode(RegistryFriendlyByteBuf buf) {
        // Читаем Holder и сразу достаем из него чистый MobEffect (.value())
        MobEffect effect = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT).decode(buf).value();

        EntityType<?> sourceType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(buf.readResourceLocation()).orElse(EntityType.PIG);

        return new SyncEffectSourcePayload(effect, sourceType);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncEffectSourcePayload msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (msg.effect() == null || msg.sourceType() == null) return;

            if (context.flow().isClientbound()) {
                SyncEffectSourceClientProxy.apply(msg.effect(), msg.sourceType());
            }
        });
    }
}
