package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class DamageContext {
    // WeakHashMap чтобы записи не держали сущности навсегда
    private static final Map<LivingEntity, Map<DamageType, Float>> CONTEXT = new WeakHashMap<>();

    private DamageContext() {}
    public static synchronized DamageType getLast(LivingEntity entity) {
        Map<DamageType, Float> map = CONTEXT.get(entity);
        if (map == null || map.isEmpty()) return null;

        // Берём первый (или любой) ключ — у тебя обычно будет один тип за раз
        return map.keySet().iterator().next();
    }
    public static synchronized void add(LivingEntity entity, DamageType type, float amount) {
        Map<DamageType, Float> map = CONTEXT.computeIfAbsent(entity, k -> new EnumMap<>(DamageType.class));
        map.merge(type, amount, Float::sum);
    }

    /**
     * Возвращает незаменяемую view-копию (пустую карту если нет записи).
     */
    public static synchronized Map<DamageType, Float> getMap(LivingEntity entity) {
        Map<DamageType, Float> map = CONTEXT.get(entity);
        if (map == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(map);
    }

    /**
     * Удаляет запись и возвращает карту (пустую, если ничего не было).
     * Используй, когда нужно "потребить" накопленный breakdown.
     */
    public static synchronized Map<DamageType, Float> consumeMap(LivingEntity entity) {
        Map<DamageType, Float> map = CONTEXT.remove(entity);
        if (map == null) return Collections.emptyMap();
        return map;
    }

    public static synchronized void clear(LivingEntity entity) {
        CONTEXT.remove(entity);
    }
}