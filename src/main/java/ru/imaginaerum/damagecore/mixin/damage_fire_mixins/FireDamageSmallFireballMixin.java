package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(SmallFireball.class)
public class FireDamageSmallFireballMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void damagecore$onHitEntity(net.minecraft.world.phys.EntityHitResult hitResult, CallbackInfo ci) {
        if (hitResult == null) return;
        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity living)) return;
        // 1) Ставим тип урона FIRE для IHasDamageType
        if (living instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.FIRE);
        }
        // 2) Добавляем в DamageContext (урон от шара Blaze = 5.0F, как в ваниле)
        DamageContext.add(living, DamageType.FIRE, 5.0F);
    }
}