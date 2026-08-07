package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.api.ModNetwork;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class EffectSourceTracker {

    private EffectSourceTracker() {}

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        // Ищем последнего атаковавшего через vanilla combat tracker
        LivingEntity lastHurtBy = player.getLastHurtByMob();
        if (lastHurtBy == null) return;

        // Исключаем игрока как источника
        EntityType<?> sourceType = lastHurtBy.getType();
        if (sourceType == EntityType.PLAYER) return;

        // Извлекаем чистый MobEffect из Holder (с помощью .value())
        MobEffect effect = event.getEffectInstance().getEffect().value();

        // ПРАВИЛЬНЫЙ СПОСОБ ОТПРАВКИ ИГРОКУ В NEOFORGE 1.21.1:
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new SyncEffectSourcePayload(effect, sourceType)
        );
    }



}