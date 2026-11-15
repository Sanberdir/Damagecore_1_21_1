package ru.imaginaerum.damagecore.damage_enchant;

public final class FireAspectConfig {
    /** Огненный урон за уровень: 0.5, 1.0 */
    public static final float FIRE_PER_LEVEL = 0.5F;
    /** Шанс поджечь за уровень: 0.25 = 25%, 0.5 = 50% */
    public static final float IGNITE_CHANCE_PER_LEVEL = 0.25F;
    /** Длительность горения в секундах */
    public static final float IGNITE_SECONDS = 4.0F;

    private FireAspectConfig() {}
}