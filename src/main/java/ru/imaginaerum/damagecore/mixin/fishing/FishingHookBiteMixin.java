package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookBiteMixin {

    /** Ванильной поклёвки нет: клюёт только настоящая рыба из FightManager. */
    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void deepfishing$noVanillaBite(BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }
}