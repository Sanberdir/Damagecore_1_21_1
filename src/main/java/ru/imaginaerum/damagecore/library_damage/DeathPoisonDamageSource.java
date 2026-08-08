package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class DeathPoisonDamageSource extends DamageSource {

    private final Entity directEntity;

    public DeathPoisonDamageSource(Holder<DamageType> damageTypeHolder, Entity directEntity) {
        super(damageTypeHolder); // Передаём Holder<DamageType>
        this.directEntity = directEntity;
    }

    @Override
    public Entity getEntity() {
        return this.directEntity;
    }
}