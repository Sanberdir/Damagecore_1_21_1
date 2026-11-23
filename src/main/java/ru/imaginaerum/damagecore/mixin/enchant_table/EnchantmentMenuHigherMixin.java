package ru.imaginaerum.damagecore.mixin.enchant_table;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_enchantments.HigherEnchants;

import net.minecraft.world.Container;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuHigherMixin {

    @Shadow
    @Final
    private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void damagecore$higherEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (id < HigherEnchants.BUTTON_BASE) return;   // ванильные кнопки 0..2 не трогаем

        boolean ok = HigherEnchants.tryApply(player, enchantSlots.getItem(0), id - HigherEnchants.BUTTON_BASE);
        if (ok) {
            enchantSlots.setChanged();
            access.execute((level, pos) -> level.playSound(null, pos,
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS,
                    1.0F, level.random.nextFloat() * 0.1F + 0.9F));
        }
        cir.setReturnValue(ok);
    }
}