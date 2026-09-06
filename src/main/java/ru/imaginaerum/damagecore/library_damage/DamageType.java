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
    POISON("poison"),
    TEMPERATURE("temperature"),
    // Звуковой урон
    SOUNDER("sounder"),
    PSY("psy"),
    BLUDGEONING("bludgeoning");

    private final String damageName;
    public boolean isPhysical() {
        return this == PIERCING || this == SLASHING || this == BLUDGEONING;
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
