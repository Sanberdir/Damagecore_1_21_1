package ru.imaginaerum.damagecore.mixin.enchant_table;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.gui_enchant.EnchantTabs;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenEnchantLabelsMixin {

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void damagecore$hideEnchantLabels(GuiGraphics gui, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object) this instanceof EnchantmentScreen && EnchantTabs.isStarTab()) {
            ci.cancel();
        }
    }
}