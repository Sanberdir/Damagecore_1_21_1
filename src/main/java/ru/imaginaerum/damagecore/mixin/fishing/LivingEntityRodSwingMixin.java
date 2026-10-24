package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRodSwingMixin {

    /** Пока поплавок в воде, у своего игрока нет замаха: ни рука, ни леска не дёргаются. */
    @Inject(method = "getAttackAnim", at = @At("HEAD"), cancellable = true)
    private void deepfishing$noRodSwing(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof Player p)) return;
        if (p != Minecraft.getInstance().player) return;
        if (p.fishing == null) return;
        if (p.getMainHandItem().getItem() instanceof FishingRodItem
                || p.getOffhandItem().getItem() instanceof FishingRodItem) {
            cir.setReturnValue(0f);
        }
    }
}