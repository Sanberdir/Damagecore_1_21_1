package ru.imaginaerum.damagecore.mixin.enchant_table;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenLayoutAccessor {
    @Accessor("imageHeight") void damagecore$setImageHeight(int h);
    @Accessor("topPos") void damagecore$setTopPos(int y);
}