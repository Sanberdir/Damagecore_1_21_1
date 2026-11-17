package ru.imaginaerum.damagecore.mixin.enchant_table;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenHoverInvoker {
    @Invoker("isHovering")
    boolean damagecore$isHovering(int x, int y, int w, int h, double mx, double my);
}