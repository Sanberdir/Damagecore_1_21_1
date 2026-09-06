package ru.imaginaerum.damagecore.mixin.immunitet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(LivingEntity.class)
public abstract class FreezeThrottleMixin {

    // дробный аккумулятор прогресса замерзания, персонально на игрока
    private static final Map<UUID, Double> damagecore$freezeAccumulator = new ConcurrentHashMap<>();

    // ordinal = 0: это ветка "isInPowderSnow -> increment" (idёт первой по байткоду,
    // т.к. компилируется из if-ветки раньше else-ветки с decrement). Ветка decrement
    // (else, i - 2) не перехватывается — замерзание оттаивает как обычно.
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setTicksFrozen(I)V",
                    ordinal = 0
            )
    )
    private void damagecore$throttleFreezeIncrement(LivingEntity self, int proposedTicks){
        if (!(self instanceof Player player)) {
            self.setTicksFrozen(proposedTicks);
            return;
        }

        int immunityPercent = PlayerStatsCapability.get(player)
                .map(IPlayerStats::getImmunityPercent)
                .orElse(0);

        if (immunityPercent <= 0) {
            self.setTicksFrozen(proposedTicks);
            return;
        }

        double interval = 1.0 + immunityPercent / 100.0;
        UUID id = player.getUUID();
        double acc = damagecore$freezeAccumulator.merge(id, 1.0, Double::sum);

        if (acc >= interval) {
            damagecore$freezeAccumulator.put(id, acc - interval);
            self.setTicksFrozen(proposedTicks); // реальный +1 применяется
        }
        // иначе тик "пропускается" — proposedTicks НЕ применяется,
        // счётчик замерзания в этот тик не растёт
    }
}