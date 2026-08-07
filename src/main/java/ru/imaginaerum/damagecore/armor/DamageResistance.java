package ru.imaginaerum.damagecore.armor;

public class DamageResistance {
    private final float flat;
    private final float percent;

    public DamageResistance(float flat, float percent) {
        this.flat = flat;
        this.percent = Math.max(0, Math.min(1, percent)); // Ограничиваем проценты 0-100%
    }

    public float apply(float damage) {
        float afterFlat = Math.max(0, damage - flat);
        return afterFlat * (1 - percent);
    }

    public float getFlat() { return flat; }
    public float getPercent() { return percent; }

    @Override
    public String toString() {
        if (flat > 0 && percent > 0) {
            return String.format("%.1f + %.0f%%", flat, percent * 100);
        } else if (flat > 0) {
            return String.format("%.1f", flat);
        } else if (percent > 0) {
            return String.format("%.0f%%", percent * 100);
        }
        return "0";
    }
}