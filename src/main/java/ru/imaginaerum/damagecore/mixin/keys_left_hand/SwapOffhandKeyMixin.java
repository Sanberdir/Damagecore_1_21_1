package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class SwapOffhandKeyMixin {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void DC$blockSwapOffhand(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        // сравниваем по имени биндинга из lang-файла
        if ("key.swapOffhand".equals(self.getName())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void DC$blockSwapOffhandClick(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if ("key.swapOffhand".equals(self.getName())) {
            cir.setReturnValue(false);
        }
    }
}