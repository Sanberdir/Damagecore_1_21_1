package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.datagen.DamageTypesGenerator;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.DeathPoisonDamageSource;

public class DeathPoisonEffect extends MobEffect {

    public DeathPoisonEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;

        float damage = 1.0F * (amplifier + 1);

        // Устанавливаем тип урона POISON на жертве
        if (entity instanceof IHasDamageType hasDamageType) {
            hasDamageType.setLastDamageType(DamageType.POISON);
        }

        // Сохраняем информацию об уроне в DamageContext
        DamageContext.add(entity, DamageType.POISON, damage);

        // Проверяем, доступен ли кастомный DamageType DEATH_POISON
        var registry = entity.level().registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isPresent()) {
            var damageTypeHolder = registry.get().getHolder(DamageTypesGenerator.POISON);

            if (damageTypeHolder.isPresent()) {
                // Используем кастомный DamageSource для death poison
                Entity sourceEntity = entity.getLastAttacker();
                if (sourceEntity == null) {
                    sourceEntity = entity;
                }

                DamageSource damageSource = new DeathPoisonDamageSource(damageTypeHolder.get(), sourceEntity);
                entity.hurt(damageSource, damage);
                return true;
            }
        }

        return true;
    }
}