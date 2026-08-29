package ru.imaginaerum.damagecore.library_damage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит тип урона, который "запланирован" текущей анимацией игрока,
 * до момента, когда реальный удар (attack/hurt) будет обработан
 * и заберёт (consume) это значение.
 */
public final class PendingAttackDamageType {
    private static final Map<UUID, DamageType> PENDING = new ConcurrentHashMap<>();

    private PendingAttackDamageType() {}

    public static void set(UUID playerId, DamageType type) {
        if (type == null) {
            PENDING.remove(playerId);
        } else {
            PENDING.put(playerId, type);
        }
    }

    public static DamageType consume(UUID playerId) {
        return PENDING.remove(playerId);
    }
}