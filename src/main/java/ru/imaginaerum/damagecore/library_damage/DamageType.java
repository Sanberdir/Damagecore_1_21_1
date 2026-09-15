package ru.imaginaerum.damagecore.library_damage;

public enum DamageType {
    PIERCING("piercing"),
    SLASHING("slashing"),
    FIRE("fire"),
    COLD("cold"),
    // Удушье
    SUFFOCATION("suffocation"),
    // Кровотечение
    BLEEDING("bleeding"),
    // Лучистый
    LUMINOUS_RADIANT("luminous_radiant"),
    NECROTIC("necrotic"),
    LIGHTNING("lightning"),
    THUNDER("thunder"),
    ETHEREAL("ethereal"),
    THERMAL("thermal"),
    WITHERING("withering"),
    POISON("poison"),
    EXPLOSIVE("explosive"),
    ACID("acid"),
    TEMPERATURE("temperature"),
    // Звуковой урон
    SOUNDER("sounder"),
    PSY("psy"),
    BLUDGEONING("bludgeoning");

    private final String damageName;
    public boolean isPhysical() {
        return this == PIERCING || this == SLASHING || this == BLUDGEONING;
    }
    public boolean isElemental() {
        return this == FIRE || this == COLD || this == LIGHTNING || this == THUNDER || this == LUMINOUS_RADIANT || this == NECROTIC || this == ETHEREAL;
    }
    public boolean isAfflictions() {
        return this == PSY || this == SUFFOCATION || this == THERMAL || this == WITHERING || this == BLEEDING;
    }
    public boolean isAlchemical() {
        return this == EXPLOSIVE || this == POISON || this == ACID;
    }
    DamageType(String damageName) {
        this.damageName = damageName;
    }

    public String getDamageName() {
        return damageName;
    }

    @Override
    public String toString() {
        return damageName;
    }
}
