package ru.imaginaerum.damagecore.entity.chest_of_wild_currents;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SunkenBarrelRenderer extends GeoEntityRenderer<SunkenBarrelEntity> {

    public SunkenBarrelRenderer(EntityRendererProvider.Context context) {
        super(context, new SunkenBarrelModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(SunkenBarrelEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float t = entity.tickCount + partialTick + entity.getId() * 13.7f;

        float sink = entity.getSinkProgress(partialTick);

        if (sink > 0f) {
            // тонет: заваливается набок и носом вниз, качка постепенно уходит
            float ease = sink * sink;
            poseStack.translate(0, 0.45, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(ease * 40f));
            poseStack.mulPose(Axis.XP.rotationDegrees(ease * 25f));
            poseStack.translate(0, -0.45, 0);
        } else if (entity.isInWater()) {
            float bob   = Mth.sin(t * 0.12f) * 0.03f;
            float roll  = Mth.sin(t * 0.09f) * 4f;
            float pitch = Mth.sin(t * 0.07f + 1.3f) * 3f;

            poseStack.translate(0, bob, 0);
            poseStack.translate(0, 0.45, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0, -0.45, 0);
        }

        // Поворот по yaw здесь не делаю: GeoEntityRenderer сам применяет entityYaw,
        // а вращение бочки задаётся в tick() через setYRot.
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
    @Override
    protected void applyRotations(SunkenBarrelEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

        float cy = entity.getBbHeight() / 2f;
        poseStack.translate(0, cy, 0);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getTiltX(partialTick)));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(entity.getTiltZ(partialTick)));
        poseStack.translate(0, -cy, 0);
    }
}