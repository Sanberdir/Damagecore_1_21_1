package ru.imaginaerum.damagecore.mixin.first_person;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority больше 2000, чтобы выполниться ПОСЛЕ миксина playeranim
@Mixin(value = PlayerRenderer.class, priority = 3000)
public abstract class FirstPersonOffsetMixin {

    // Координаты в блоках, в системе тела игрока: X - влево/вправо, Y - вверх/вниз, Z - вперёд/назад.
    // Меняйте по одному значению за запуск.
    private static final float OFFSET_X = 0.0f;
    private static final float OFFSET_Y = 0.0f;
    private static final float OFFSET_Z = 0.3f;

    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("RETURN")
    )
    private void damagecore$firstPersonOffset(AbstractClientPlayer player, PoseStack poseStack,
                                              float f, float bodyYaw, float tickDelta, float scale,
                                              CallbackInfo ci) {
        if (FirstPersonMode.isFirstPersonPass() && player == Minecraft.getInstance().cameraEntity) {
            poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);
        }
    }
}