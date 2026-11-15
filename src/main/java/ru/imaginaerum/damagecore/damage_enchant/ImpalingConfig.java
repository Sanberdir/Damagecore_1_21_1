package ru.imaginaerum.damagecore.damage_enchant;

public final class ImpalingConfig {
    /** Колющий урон по цели в воде за уровень: 0.5, 1.0, 1.5, 2.0, 2.5 */
    public static final float PIERCING_PER_LEVEL = 0.5F;
    /** Шанс замедления за уровень: 0.15 = 15% */
    public static final float SLOW_CHANCE_PER_LEVEL = 0.15F;
    /** Длительность замедления, секунды */
    public static final int SLOW_SECONDS = 3;
    /** Уровень замедления (0 = Замедление I) */
    public static final int SLOW_AMPLIFIER = 0;

    private ImpalingConfig() {}
}