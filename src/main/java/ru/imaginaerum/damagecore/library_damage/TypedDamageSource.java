package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class TypedDamageSource extends DamageSource {

    private final List<DamageType> damageCoreTypes;

    public TypedDamageSource(
            Holder<net.minecraft.world.damagesource.DamageType> vanillaType,
            List<DamageType> damageCoreTypes,
            Entity attacker
    ) {
        super(vanillaType, attacker);
        this.damageCoreTypes = damageCoreTypes;
    }

    public List<DamageType> getDamageCoreTypes() {
        return damageCoreTypes;
    }
}