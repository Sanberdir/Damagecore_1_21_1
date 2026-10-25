package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

@Mixin(Entity.class)
public abstract class EntityWetMixin {

    /** Рыба боя на клиенте всегда «в воде», чтобы рендерер не клал её набок. */
    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void deepfishing$fishAlwaysWet(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof AbstractFish && self.level().isClientSide && FightClient.active) {
            cir.setReturnValue(true);
        }
    }
}