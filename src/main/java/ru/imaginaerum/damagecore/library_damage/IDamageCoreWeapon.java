package ru.imaginaerum.damagecore.library_damage;

import java.util.Map;


public interface IDamageCoreWeapon {
    Map<DamageType, Double> damagecore$getDamageMap();
    void damagecore$setCustomDamage(Map<DamageType, Double> customDamage);
    void damagecore$setDefaultDamage(Map<DamageType, Double> defaultDamage); // ← новый метод
    boolean damagecore$hasCustomDamage();
}