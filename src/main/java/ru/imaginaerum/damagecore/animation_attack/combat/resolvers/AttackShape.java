package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

public enum AttackShape {
    /** Точечный — по прицелу (piercing). */
    SINGLE,
    /** Конус вперёд (slashing). */
    CONE,
    /** Круг 360° вокруг игрока. */
    CIRCLE;

    public static AttackShape fromName(String s) {
        if (s == null) return SINGLE;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return SINGLE; }
    }
}