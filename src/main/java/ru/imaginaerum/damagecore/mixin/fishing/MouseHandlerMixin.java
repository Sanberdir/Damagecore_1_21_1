package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

/**
 * While a fish is on the hook, the camera is frozen: raw mouse movement is diverted
 * into {@link FightClient} (and later sent to the server) instead of turning the player.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "onMove", at = @At("TAIL"))
    private void deepfishing$captureMouse(long window, double x, double y, CallbackInfo ci) {
        if (FightClient.active) {
            FightClient.addMouse(accumulatedDX, accumulatedDY);
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
        }
    }
}
