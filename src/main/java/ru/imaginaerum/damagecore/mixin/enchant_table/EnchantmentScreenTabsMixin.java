package ru.imaginaerum.damagecore.mixin.enchant_table;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.gui_enchant.EnchantTabs;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenTabsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$initTabs(CallbackInfo ci) {
        EnchantTabs.reset();
        EnchantTabs.applyLayout((EnchantmentScreen) (Object) this);
    }

    // Вместо ванильного фона (стол, книга, полосы чар) рисуем свою панель
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void damagecore$starBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY,
                                   CallbackInfo ci) {
        if (!EnchantTabs.isStarTab()) return;
        EnchantmentScreen s = (EnchantmentScreen) (Object) this;
        EnchantTabs.renderStarPanel(gui, s.getGuiLeft(), s.getGuiTop());
        ci.cancel();
    }@WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentScreen;isHovering(IIIIDD)Z"))
    private boolean damagecore$noClueHover(EnchantmentScreen instance, int x, int y, int w, int h,
                                           double mx, double my, Operation<Boolean> original) {
        if (EnchantTabs.isStarTab()) return false;
        return original.call(instance, x, y, w, h, mx, my);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderTabs(GuiGraphics gui, int mouseX, int mouseY,
                                       float partialTick, CallbackInfo ci) {
        EnchantmentScreen s = (EnchantmentScreen) (Object) this;
        EnchantTabs.renderTabs(gui, s.getGuiLeft(), s.getGuiTop());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$tabsClick(double mouseX, double mouseY, int button,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (EnchantTabs.mouseClicked(mouseX, mouseY, button, (EnchantmentScreen) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(
            method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/EnchantmentMenu;clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z"))
    private boolean damagecore$noEnchantClick(EnchantmentMenu menu, Player player, int id,
                                              Operation<Boolean> original) {
        if (EnchantTabs.isStarTab()) return false;
        return original.call(menu, player, id);
    }
}