package ru.imaginaerum.damagecore.mixin.damage_bleeding_mixins;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityBleedingAnimationMixin {

    private static boolean damagecore$suppressHurt = false;

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void damagecore$handleBleedingEvent(byte id, CallbackInfo ci) {
        if (id == 123) {
            damagecore$suppressHurt = true;
            ci.cancel();
        }
    }

    @Inject(method = "animateHurt", at = @At("HEAD"), cancellable = true)
    private void damagecore$suppressBleedingAnimation(float yaw, CallbackInfo ci) {
        if (damagecore$suppressHurt) {
            damagecore$suppressHurt = false;
            ci.cancel();
        }
    }
}