package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;

@Mixin(KeyMapping.class)
public abstract class BlockHotbarKeysMixin {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void DC$blockHotbarDown(CallbackInfoReturnable<Boolean> cir) {
        damagecore$maybeBlock(cir);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void DC$blockHotbarClick(CallbackInfoReturnable<Boolean> cir) {
        damagecore$maybeBlock(cir);
    }

    @Unique
    private void damagecore$maybeBlock(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof ICombatModeEntity combatEntity) || !combatEntity.damagecore$isCombatMode()) return;

        String name = ((KeyMapping) (Object) this).getName();
        if (name.startsWith("key.hotbar")) { // key.hotbar.1 .. key.hotbar.9
            cir.setReturnValue(false);
        }
    }
}