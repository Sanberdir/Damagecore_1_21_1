package ru.imaginaerum.damagecore.entity.sunken_lockbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

public class SunkenLockboxRenderer extends GeoEntityRenderer<SunkenLockboxEntity> {
    private static final int FADE_TICKS = 40;   // 2 секунды

    public SunkenLockboxRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(MODID, "sunken_lockbox")));
        this.shadowRadius = 0.4f;
    }

    private static boolean fading(SunkenLockboxEntity e) {
        return e.isFishingVisual() && e.tickCount < FADE_TICKS;
    }

    private static float fadeAlpha(SunkenLockboxEntity e, float partialTick) {
        float k = Mth.clamp((e.tickCount + partialTick) / FADE_TICKS, 0f, 1f);
        return k * k * (3f - 2f * k);   // плавный старт и финиш
    }

    // прозрачный слой только на время появления, потом обычный
    @Override
    public RenderType getRenderType(SunkenLockboxEntity e, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return fading(e) ? RenderType.entityTranslucent(texture)
                : super.getRenderType(e, texture, bufferSource, partialTick);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, SunkenLockboxEntity e, BakedGeoModel model,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, int colour) {
        if (fading(e)) {
            int alpha = (int) (fadeAlpha(e, partialTick) * 255f);
            colour = (alpha << 24) | (colour & 0x00FFFFFF);
        }
        super.actuallyRender(poseStack, e, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void render(SunkenLockboxEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // тень тоже проявляется, а не торчит у невидимого сундука
        this.shadowRadius = fading(entity) ? 0.4f * fadeAlpha(entity, partialTick) : 0.4f;

        float t = entity.tickCount + partialTick + entity.getId() * 13.7f;

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        if (entity.isInWater()) {
            float bob   = Mth.sin(t * 0.12f) * 0.03f;
            float roll  = Mth.sin(t * 0.09f) * 4f;
            float pitch = Mth.sin(t * 0.07f + 1.3f) * 3f;

            poseStack.translate(0, bob, 0);
            poseStack.translate(0, 0.45, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0, -0.45, 0);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}