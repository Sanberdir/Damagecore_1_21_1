package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class RemoveVanillaOffhandKeyMixin {

    @Shadow @Final @Mutable
    public KeyMapping[] keyMappings;

    @Shadow @Final
    public KeyMapping keySwapOffhand;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void damagecore$removeOffhandKeyBinding(CallbackInfo ci) {
        // Удаляем ванильную кнопку смены рук из общего массива настроек игры
        if (this.keyMappings != null && this.keySwapOffhand != null) {
            this.keyMappings = ArrayUtils.removeElement(this.keyMappings, this.keySwapOffhand);
        }
    }
}
