package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class TypedDamageSource extends DamageSource {

    private final DamageType damageCoreType;

    public TypedDamageSource(
            Holder<net.minecraft.world.damagesource.DamageType> vanillaType,
            DamageType damageCoreType,
            Entity attacker
    ) {
        super(vanillaType, attacker);
        this.damageCoreType = damageCoreType;
    }

    public DamageType getDamageCoreType() {
        return damageCoreType;
    }
}