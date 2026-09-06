package ru.imaginaerum.damagecore.mixin.immunitet;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    // Ванильный базовый интервал яда (25 тиков при amplifier=0, делится пополам на каждый следующий уровень зелья)
    private static final int POISON_BASE_INTERVAL = 25;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"
            )
    )
    private boolean damagecore$redirectPoisonInterval(
            MobEffect effectInstance, int duration, int amplifier,
            LivingEntity entity, Runnable onExpired) {

        // Не яд — не трогаем, отдаём ванильному коду как обычно
        if (effectInstance != MobEffects.POISON.value()) {
            return effectInstance.shouldApplyEffectTickThisTick(duration, amplifier);
        }

        int immunityPercent = (entity instanceof Player player)
                ? PlayerStatsCapability.get(player)
                .map(IPlayerStats::getImmunityPercent)
                .orElse(0)
                : 0;

        int interval = (int) Math.round(POISON_BASE_INTERVAL * (1.0 + immunityPercent / 100.0)) >> amplifier;
        if (interval <= 0) interval = 1;

        return duration % interval == 0;
    }
}