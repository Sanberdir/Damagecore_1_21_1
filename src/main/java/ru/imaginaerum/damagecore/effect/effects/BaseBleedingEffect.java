package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

public abstract class BaseBleedingEffect extends MobEffect {

    private final float damageAmount;
    private int cachedLiveForceLevel = 0;

    public BaseBleedingEffect(MobEffectCategory category, int color, float damageAmount) {
        super(category, color);
        this.damageAmount = damageAmount;
    }

    protected abstract int getBaseInterval();

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = (getBaseInterval() + cachedLiveForceLevel * 5) >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;

        // Обновляем кеш перед следующим shouldApplyEffectTickThisTick
        cachedLiveForceLevel = PlayerStatsCapability.get((Player) entity)
                .map(s -> s.getStat(StatsType.LIVE_FORCE)).orElse(0);

        entity.level().broadcastEntityEvent(entity, (byte) 123);

        float newHealth = Math.max(entity.getHealth() - damageAmount, 0.0f);
        entity.setHealth(newHealth);
        entity.hurtTime = 0;
        entity.invulnerableTime = 0;

        return true;
    }
}