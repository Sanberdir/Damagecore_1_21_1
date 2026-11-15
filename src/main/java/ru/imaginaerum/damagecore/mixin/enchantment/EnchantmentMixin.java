package ru.imaginaerum.damagecore.mixin.enchantment;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Unique
    private boolean dc$isChanneling() {
        Component desc = ((Enchantment) (Object) this).description();
        return desc.getContents() instanceof TranslatableContents tc
                && "enchantment.minecraft.channeling".equals(tc.getKey());
    }
    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void damagecore$elementalExclusive(Holder<Enchantment> a, Holder<Enchantment> b,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (EnchantmentElements.conflicts(a, b)) {
            cir.setReturnValue(false);
        }
    }
    @Unique
    private static boolean dc$isMelee(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
    }

    // наковальня, /enchant
    @Inject(method = "isSupportedItem", at = @At("RETURN"), cancellable = true)
    private void dc$supported(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && dc$isChanneling() && dc$isMelee(stack)) {
            cir.setReturnValue(true);
        }
    }

    // стол чарования
    @Inject(method = "isPrimaryItem", at = @At("RETURN"), cancellable = true)
    private void dc$primary(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && dc$isChanneling() && dc$isMelee(stack)) {
            cir.setReturnValue(true);
        }
    }
}