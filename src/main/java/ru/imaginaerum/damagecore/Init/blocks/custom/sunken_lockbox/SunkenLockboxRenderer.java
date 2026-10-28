package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SunkenLockboxRenderer extends GeoBlockRenderer<SunkenLockboxEntity> {
    public SunkenLockboxRenderer() {
        super(new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sunken_lockbox")));
    }

    @Override
    public void render(SunkenLockboxEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        int segment = be.getBlockState().getValue(SunkenLockboxBlock.ROTATION);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-RotationSegment.convertToDegrees(segment)));
        poseStack.translate(-0.5, 0, -0.5);
        super.render(be, partialTick, poseStack, buffer, light, overlay);
        poseStack.popPose();
    }
}