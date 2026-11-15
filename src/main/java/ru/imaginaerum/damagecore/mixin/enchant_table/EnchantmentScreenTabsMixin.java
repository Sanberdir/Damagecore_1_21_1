package ru.imaginaerum.damagecore.mixin.enchant_table;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.gui_enchant.EnchantTabs;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenTabsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$resetTabs(CallbackInfo ci) {
        EnchantTabs.reset();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderTabs(GuiGraphics gui, int mouseX, int mouseY,
                                       float partialTick, CallbackInfo ci) {
        EnchantmentScreen s = (EnchantmentScreen) (Object) this;
        EnchantTabs.render(gui, s.getGuiLeft(), s.getGuiTop(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$tabsClick(double mouseX, double mouseY, int button,
                                      CallbackInfoReturnable<Boolean> cir) {
        EnchantmentScreen s = (EnchantmentScreen) (Object) this;
        if (EnchantTabs.mouseClicked(mouseX, mouseY, button, s.getGuiLeft(), s.getGuiTop())) {
            cir.setReturnValue(true);
        }
    }
}