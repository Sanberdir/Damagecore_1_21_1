// EffectTextUtils.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import ru.imaginaerum.damagecore.library_damage.DamageType;

/** Мелкие текстовые хелперы, общие для всех тултипов вкладки эффектов. */
public final class EffectTextUtils {
    private EffectTextUtils() {}

    public static String formatTicks(int ticks) {
        int sec = ticks / 20;
        if (sec >= 60) return (sec / 60) + ":" + String.format("%02d", sec % 60);
        return sec + "s";
    }

    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    public static String getDamageTypeKey(DamageType type) {
        return switch (type) {
            case PIERCING -> "damagecore.damage_type.piercing";
            case SLASHING -> "damagecore.damage_type.slashing";
            case FIRE -> "damagecore.damage_type.fire";
            case THUNDER -> "damagecore.damage_type.thunder";
            case THERMAL -> "damagecore.damage_type.thermal";
            case WITHERING -> "damagecore.damage_type.withering";
            case EXPLOSIVE -> "damagecore.damage_type.explosive";
            case ACID -> "damagecore.damage_type.acid";
            case ETHEREAL -> "damagecore.damage_type.etherial";
            case COLD -> "damagecore.damage_type.cold";
            case SUFFOCATION -> "damagecore.damage_type.suffocation";
            case BLEEDING -> "damagecore.damage_type.bleeding";
            case LUMINOUS_RADIANT -> "damagecore.damage_type.luminous_radiant";
            case TEMPERATURE -> "damagecore.damage_type.temperature";
            case NECROTIC -> "damagecore.damage_type.necrotic";
            case LIGHTNING -> "damagecore.damage_type.lightning";
            case POISON -> "damagecore.damage_type.poison";
            case SOUNDER -> "damagecore.damage_type.sounder";
            case PSY -> "damagecore.damage_type.psy";
            case BLUDGEONING -> "damagecore.damage_type.bludgeoning";
        };
    }
}