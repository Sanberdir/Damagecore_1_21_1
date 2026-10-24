package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSchoolingFish.class)
public abstract class AbstractSchoolingFishMixin {

    @Shadow private AbstractSchoolingFish leader;

    /** Повторный stopFollowing (после нашего detachSchool) не должен падать на null-лидере. */
    @Inject(method = "stopFollowing", at = @At("HEAD"), cancellable = true)
    private void deepfishing$safeStop(CallbackInfo ci) {
        if (this.leader == null) ci.cancel();
    }
}