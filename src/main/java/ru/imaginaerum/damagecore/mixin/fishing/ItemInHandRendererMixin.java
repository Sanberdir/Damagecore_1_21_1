package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private float oOffHandHeight;

    /** Пока поплавок в воде, клики по удочке не должны опускать руку. */
    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void deepfishing$noDip(InteractionHand hand, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        if (p != null && p.fishing != null && p.getItemInHand(hand).getItem() instanceof FishingRodItem) {
            ci.cancel();
        }
    }

    /** Любая просадка руки с удочкой (клик, пересчёт силы удара) гасится: рука всегда на полной высоте. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void deepfishing$holdRod(CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        if (p == null || p.fishing == null) return;
        if (p.getMainHandItem().getItem() instanceof FishingRodItem) {
            mainHandHeight = 1f;
            oMainHandHeight = 1f;
        }
        if (p.getOffhandItem().getItem() instanceof FishingRodItem) {
            offHandHeight = 1f;
            oOffHandHeight = 1f;
        }
    }
}