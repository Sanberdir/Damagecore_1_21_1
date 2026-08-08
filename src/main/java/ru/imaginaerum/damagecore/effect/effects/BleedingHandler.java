package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.effect.DCEffects;

@EventBusSubscriber(
        modid = Damagecore_1_21_1_neo.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class BleedingHandler {

    private static final int BLEEDING_2_DURATION_TICKS = 15 * 20;

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // Проверяем Кровотечение I
        MobEffectInstance bleeding1 =
                entity.getEffect(DCEffects.BLEEDING_1);

        if (bleeding1 != null) {

            // Кровотечение I заканчивается
            if (bleeding1.getDuration() <= 1) {

                replaceBleedingEffect(
                        entity,
                        bleeding1,
                        DCEffects.BLEEDING_2,
                        BLEEDING_2_DURATION_TICKS
                );
            }

            return;
        }

        // Проверяем Кровотечение II
        MobEffectInstance bleeding2 =
                entity.getEffect(DCEffects.BLEEDING_2);

        if (bleeding2 != null) {

            // Кровотечение II заканчивается
            if (bleeding2.getDuration() <= 1) {

                replaceBleedingEffect(
                        entity,
                        bleeding2,
                        DCEffects.BLEEDING_3,
                        Integer.MAX_VALUE
                );
            }
        }
    }

    /**
     * Безопасно заменяет один эффект кровотечения на другой,
     * сохраняя усилитель и параметры старого эффекта.
     */
    private static void replaceBleedingEffect(
            LivingEntity entity,
            MobEffectInstance oldEffect,
            Holder<MobEffect> newEffect,
            int newDuration
    ) {

        // Снимаем старый эффект
        entity.removeEffect(oldEffect.getEffect());

        // Накладываем новый эффект
        entity.addEffect(
                new MobEffectInstance(
                        newEffect,
                        newDuration,
                        oldEffect.getAmplifier(),
                        oldEffect.isAmbient(),
                        oldEffect.isVisible(),
                        oldEffect.showIcon()
                )
        );
    }
}