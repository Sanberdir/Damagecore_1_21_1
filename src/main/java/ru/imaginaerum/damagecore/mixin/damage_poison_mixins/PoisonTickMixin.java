package ru.imaginaerum.damagecore.mixin.damage_poison_mixins;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mixin(MobEffectInstance.class)
public class PoisonTickMixin {

    // В 1.21.1 тип изменен на Holder<MobEffect>
    @Shadow private Holder<MobEffect> effect;
    @Shadow private int duration;
    @Shadow private int amplifier;

    @Unique
    private int damagecore_poisonSkipTicks = 0;

    /**
     * В 1.21.1 основной метод обновления эффекта называется tick.
     * Он возвращает boolean (должен ли эффект продолжать действовать).
     */
    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore_throttlePoisonTick(LivingEntity entity, Runnable onExpired, CallbackInfoReturnable<Boolean> cir) {
        // Проверяем Holder на совпадение с ядом
        if (!this.effect.is(MobEffects.POISON)) return;
        if (!(entity instanceof Player player)) return;

        // Логику пропускаем только на серверной стороне (для расчетов урона)
        if (entity.level().isClientSide) return;

        // Получаем характеристику LIVE_FORCE
        int liveForce = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.LIVE_FORCE)).orElse(0);
        if (liveForce <= 0) return;

        // В 1.21.1 метод проверки "должен ли яд нанести урон в этот тик" принимает ServerLevel
        if (entity.level() instanceof ServerLevel serverLevel) {

            // Получаем чистый MobEffect из Holder через .value()
            MobEffect mobEffect = this.effect.value();

            if (mobEffect.shouldApplyEffectTickThisTick(this.duration, this.amplifier)) {

                if (damagecore_poisonSkipTicks < liveForce) {
                    damagecore_poisonSkipTicks++;

                    // Внутренний приватный метод уменьшения длительности (или меняем duration напрямую через тень)
                    this.duration--;

                    // Возвращаем true, чтобы эффект не удалился, но прерываем ванильное нанесение урона
                    cir.setReturnValue(true);
                } else {
                    damagecore_poisonSkipTicks = 0; // Счетчик сброшен, урон проходит
                }
            }
        }
    }
}
