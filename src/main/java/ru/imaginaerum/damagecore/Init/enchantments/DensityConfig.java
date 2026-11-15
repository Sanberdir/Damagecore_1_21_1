package ru.imaginaerum.damagecore.Init.enchantments;

public final class DensityConfig {
    /** Прибавка к дробящему урону за уровень (если дробящий уже есть): 0.5, 1.0, 1.5... */
    public static final float BLUDGEONING_PER_LEVEL = 0.5F;
    /**
     * Если у оружия нет дробящего урона:
     * уровень I даёт ABSENT_BASE, каждый следующий уровень ещё +BLUDGEONING_PER_LEVEL.
     * 1.0F -> 1.0, 1.5, 2.0...   (вариант "дать 1, потом +0.5")
     * 0.5F -> 0.5, 1.0, 1.5...   (вариант "сразу по 0.5 за уровень")
     */
    public static final float ABSENT_BASE = 1.0F;

    /** Множитель дробящего урона за каждый блок падения: 0.1 = +10% */
    public static final float HEIGHT_MULT_PER_BLOCK = 0.1F;
    /** Максимальная прибавка к множителю высоты (3.0 = +300%) */
    public static final float HEIGHT_MULT_MAX = 3.0F;

    /** Изменение скорости атаки за уровень (должно совпадать с density.json) */
    public static final float ATTACK_SPEED_PER_LEVEL = -0.1F;
    /** Прибавка к требованию силы за уровень */
    public static final int STRENGTH_REQ_PER_LEVEL = 1;

    public static float bludgeoningBonus(int level, boolean weaponHasBludgeoning) {
        if (weaponHasBludgeoning) return BLUDGEONING_PER_LEVEL * level;
        return ABSENT_BASE + BLUDGEONING_PER_LEVEL * (level - 1);
    }

    private DensityConfig() {}
}