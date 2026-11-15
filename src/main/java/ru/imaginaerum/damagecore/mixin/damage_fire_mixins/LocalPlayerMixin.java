package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.WeaponAttackAnimationHandler;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD"), cancellable = true)
    private void damagecore$blockSwingWhileBusy(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (WeaponAttackAnimationHandler.isBusy(self)) ci.cancel();
    }
}