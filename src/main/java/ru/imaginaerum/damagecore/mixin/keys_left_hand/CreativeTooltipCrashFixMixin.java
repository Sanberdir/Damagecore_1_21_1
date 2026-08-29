package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeTooltipCrashFixMixin {


    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void damagecore$avoidCreativeTooltipCrash(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        cir.setReturnValue(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
    }
}