package ru.imaginaerum.damagecore.library_stats;
public enum StatsType {

    LIVE_FORCE("live_forge", 0),
    ENDURANCE("endurance",   0),
    MIND("mind",             0),
    STRENGTH("strength",     0),
    DEXTERITY("dexterity",   0),
    WISDOM("wisdom",         0);

    private final String id;
    private final int defaultValue;

    StatsType(String id, int defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    public String getId() { return id; }

    public int getDefaultValue() { return defaultValue; }

    public String getTranslationKey() { return "damagecore.stat." + id; }

    public static StatsType fromId(String id) {
        for (StatsType stat : values()) {
            if (stat.id.equals(id)) return stat;
        }
        return null;
    }

    @Override
    public String toString() { return id; }
}