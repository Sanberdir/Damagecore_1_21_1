package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.Init.fishing.food.FishFood;

@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void df$keepWeight(SingleRecipeInput input, HolderLookup.Provider regs,
                               CallbackInfoReturnable<ItemStack> cir) {
        float kg = FishFood.getKg(input.item());
        if (kg > 0f) FishFood.apply(cir.getReturnValue(), kg);
    }
}