package ru.imaginaerum.damagecore.mixin.immunitet;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(LivingEntity.class)
public abstract class SuffocationThrottleMixin {

    // Ключ: UUID игрока + тип урона (drown / in_wall) — раздельные накопители,
    // чтобы утопление и засыпание не мешали друг другу.
    private static final Map<String, Double> damagecore$suffocationAccumulator = new ConcurrentHashMap<>();

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void damagecore$throttleSuffocation(DamageSource source, float amount,
                                                CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide) return;

        boolean isDrown = source.is(DamageTypes.DROWN);
        boolean isInWall = source.is(DamageTypes.IN_WALL);
        if (!isDrown && !isInWall) return; // не наш случай — ничего не трогаем

        int endurancePercent = PlayerStatsCapability.get(player)
                .map(IPlayerStats::getEnduranceImmunityPercent)
                .orElse(0);

        if (endurancePercent <= 0) return; // нет иммунитета — ванильное поведение как есть

        String key = player.getUUID() + (isDrown ? ":drown" : ":in_wall");
        double interval = 1.0 + endurancePercent / 100.0;

        double acc = damagecore$suffocationAccumulator.merge(key, 1.0, Double::sum);

        if (acc >= interval) {
            // порог накоплен — пропускаем урон как обычно, счётчик уменьшаем на interval,
            // а не сбрасываем в 0, чтобы не терять "накопленный остаток" (плавность)
            damagecore$suffocationAccumulator.put(key, acc - interval);
        } else {
            // порог ещё не набран — эта попытка урона "проглатывается" целиком
            cir.setReturnValue(false);
        }
    }
}