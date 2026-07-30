package ru.imaginaerum.damagecore.library_stats;

public interface IPlayerStats {

    int getStat(StatsType type);
    void setStat(StatsType type, int value);

    int getPressCount(StatsType type);
    void setPressCount(StatsType type, int count);

    int MAX_LEVEL = 99;
    int BASE_COST = 9;
    double COST_MULTIPLIER = 1.09;

    // ✅ Суммарное число нажатий по всем статам
    default int getTotalPressCount() {
        int total = 0;
        for (StatsType type : StatsType.values()) {
            total += getPressCount(type);
        }
        return total;
    }

    // ✅ Стоимость зависит от общего числа вложений, а не от конкретного стата
    default int getNextCost(StatsType type) {
        return (int) Math.round(BASE_COST * Math.pow(COST_MULTIPLIER, getTotalPressCount()));
    }

    // ✅ Возврат — стоимость последнего вложения (total - 1)
    default int getRefundCost(StatsType type) {
        int total = getTotalPressCount();
        if (total <= 0) return 0;
        return (int) Math.round(BASE_COST * Math.pow(COST_MULTIPLIER, total - 1));
    }

    default boolean isMaxLevel(StatsType type) {
        return getPressCount(type) >= MAX_LEVEL;
    }
}