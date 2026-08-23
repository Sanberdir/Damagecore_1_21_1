package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;

@Mixin(MouseHandler.class)
public abstract class BlockScrollHotbarMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void DC$blockScrollHotbarSwitch(long handle, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof ICombatModeEntity combatEntity && combatEntity.damagecore$isCombatMode()) {
            ci.cancel();
        }
    }
}