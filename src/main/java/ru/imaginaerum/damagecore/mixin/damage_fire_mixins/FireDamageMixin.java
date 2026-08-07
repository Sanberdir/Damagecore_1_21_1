package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(Entity.class)
public abstract class FireDamageMixin implements IHasDamageType {

    @Unique
    private DamageType damagecore$lastDamageType;

    // ===== IHasDamageType =====

    @Override
    public DamageType getLastDamageType() {
        return this.damagecore$lastDamageType;
    }

    @Override
    public void setLastDamageType(DamageType type) {
        this.damagecore$lastDamageType = type;
    }

    // ===== Lava damage =====
    @Inject(
            method = "baseTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 0 // первый вызов hurt в baseTick() — это onFire()
            )
    )
    private void damagecore$onFireDamage(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        this.setLastDamageType(DamageType.FIRE);

        if (self instanceof LivingEntity living) {
            DamageContext.add(living, DamageType.FIRE, 1.0F);
        }

    }
    @Inject(method = "lavaHurt", at = @At("HEAD"))
    private void damagecore$onLavaHurt(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 1) Устанавливаем тип урона
        this.setLastDamageType(DamageType.FIRE);


        // 3) Если это LivingEntity — сразу кладём в DamageContext
        if (self instanceof LivingEntity living) {
            // Ванильное значение урона от лавы
            DamageContext.add(living, DamageType.FIRE, 4.0F);
        }
    }
}