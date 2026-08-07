package ru.imaginaerum.damagecore.mixin.damage_explosion_mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(Explosion.class)
public class ExplosionDamageTrackingMixin {

    @Redirect(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean damagecore$controlExplosionDamage(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return entity.hurt(source, amount);
        }

        // 1. Устанавливаем тип урона
        if (livingEntity instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.BLUDGEONING);
        }

        // 2. Добавляем в DamageContext
        DamageContext.add(livingEntity, DamageType.BLUDGEONING, amount);

        // 3. Применяем модификаторы (например, уменьшение на 50%)
        float modifiedDamage = amount * 0.5F;

        // 4. Вызываем оригинальный метод с модифицированным уроном
        return entity.hurt(source, modifiedDamage);
    }
}