package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public abstract class BaseBleedingEffect extends MobEffect {

    private final float damageAmount;

    // per-entity кеш, а не общий на весь эффект (MobEffect — синглтон на все сущности)
    private final Map<UUID, Integer> cachedImmunityPercent = new ConcurrentHashMap<>();

    public BaseBleedingEffect(MobEffectCategory category, int color, float damageAmount) {
        super(category, color);
        this.damageAmount = damageAmount;
    }

    protected abstract int getBaseInterval();

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Здесь нет доступа к entity, поэтому используем последний закешированный %.
        // Обновляется он в applyEffectTick ниже, перед следующей проверкой интервала.
        int percent = lastKnownPercent;
        int interval = (int) Math.round(getBaseInterval() * (1.0 + percent / 100.0)) >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    // используется как "текущий" % для метода выше — устанавливается непосредственно
    // перед вызовом shouldApplyEffectTickThisTick для конкретной entity через applyEffectTick
    private int lastKnownPercent = 0;

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;

        int percent = (entity instanceof Player player)
                ? PlayerStatsCapability.get(player)
                .map(s -> s.getImmunityPercent())
                .orElse(0)
                : 0;

        cachedImmunityPercent.put(entity.getUUID(), percent);
        lastKnownPercent = percent;

        entity.level().broadcastEntityEvent(entity, (byte) 123);

        float newHealth = Math.max(entity.getHealth() - damageAmount, 0.0f);
        entity.setHealth(newHealth);
        entity.hurtTime = 0;
        entity.invulnerableTime = 0;

        return true;
    }
}