package ru.imaginaerum.damagecore.mixin.damage_poison_mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public class PoisonEffectsMixin {

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean damagecore$onPoisonEffectTick(
            LivingEntity entity,
            DamageSource source,
            float amount
    ) {
        if (entity.getHealth() > 1.0F) {
            return handlePoisonDamage(entity, source, amount);
        }

        return entity.hurt(source, amount);
    }

    private boolean handlePoisonDamage(
            LivingEntity entity,
            DamageSource source,
            float amount
    ) {
        if (entity instanceof IHasDamageType hasDamageType) {
            hasDamageType.setLastDamageType(DamageType.POISON);
        }

        DamageContext.add(
                entity,
                DamageType.POISON,
                amount
        );

        return entity.hurt(source, amount);
    }
}