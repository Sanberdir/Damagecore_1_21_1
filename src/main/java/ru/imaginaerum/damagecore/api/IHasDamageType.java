package ru.imaginaerum.damagecore.api;

import ru.imaginaerum.damagecore.library_damage.DamageType;

public interface IHasDamageType {
    DamageType getLastDamageType();
    void setLastDamageType(DamageType type);
}
