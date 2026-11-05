package ru.imaginaerum.damagecore.mixin.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.HoverRays;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    /** Висящий лут из сундука (без гравитации, не беззвучный) получает маленькие лучи. */
    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void deepfishing$hoverRays(ItemEntity e, float yaw, float pt, PoseStack ps,
                                       MultiBufferSource buf, int light, CallbackInfo ci) {
        if (!e.isNoGravity() || e.isSilent()) return;
        float t = e.getAge() + pt;
        float cy = 0.225f + 0.1f * Mth.sin(t / 10f + e.bobOffs);
        HoverRays.render(ps, buf, t + e.getId() * 7f, cy);
    }
}