package ru.imaginaerum.damagecore.library_ranks;

/**
 * Библиотека рангов с фиксированными множителями.
 * Иерархия силы: S > A > B > C > D > E.
 * Значения множителей заданы константно согласно таблице:
 * E - 2, D - 3, C - 4, B - 5, A - 7, S - 10.
 */
public enum DamageRank {

    // Порядок объявления соответствует иерархии от высшего к низшему (ordinal() = 0 у S)
    S(10),
    A(7),
    B(5),
    C(4),
    D(3),
    E(2);

    // Константный множитель, привязанный к рангу
    private final int multiplier;

    DamageRank(int multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Возвращает множитель, соответствующий рангу.
     */
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * Проверяет, выше ли этот ранг другого (по иерархии S > A > B > C > D > E).
     * Сравнение через ordinal() корректно, так как порядок объявления enum
     * строго соответствует иерархии силы рангов.
     */
    public boolean isHigherThan(DamageRank other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Проверяет, ниже ли этот ранг другого.
     */
    public boolean isLowerThan(DamageRank other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Безопасный поиск ранга по буквенному имени (S, A, B, C, D, E),
     * без учёта регистра. Возвращает null, если ранг не найден.
     * Полезно при парсинге рангов из конфигов, NBT/attachment-данных и т.д.
     */
    public static DamageRank fromName(String name) {
        if (name == null) {
            return null;
        }
        for (DamageRank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name() + " (" + multiplier + ")";
    }
}