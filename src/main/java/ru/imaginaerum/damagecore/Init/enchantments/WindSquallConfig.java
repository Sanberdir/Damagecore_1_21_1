package ru.imaginaerum.damagecore.Init.enchantments;

public final class WindSquallConfig {
    /** Минимальная дистанция падения для срабатывания, блоки */
    public static final float MIN_FALL_DISTANCE = 1.5F;
    /** Вертикальная скорость подброса: база + за уровень (прыжок игрока ~0.42) */
    public static final double LAUNCH_BASE = 0.6D;
    public static final double LAUNCH_PER_LEVEL = 0.2D;
    /** Прибавка к шансу крита в падении за уровень: 0.10 = 10% */
    public static final float CRIT_CHANCE_PER_LEVEL = 0.10F;
    /** Множитель крита (ванильный) */
    public static final float CRIT_MULT = 1.5F;

    private WindSquallConfig() {}
}