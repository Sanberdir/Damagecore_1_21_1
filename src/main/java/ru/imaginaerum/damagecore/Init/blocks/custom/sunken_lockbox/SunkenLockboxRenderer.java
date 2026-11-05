package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SunkenLockboxRenderer extends GeoBlockRenderer<SunkenLockboxEntity> {
    public SunkenLockboxRenderer() {
        super(new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sunken_lockbox")));
        addRenderLayer(new SunkenLockboxGlowLayer(this));
    }

    @Override
    public void render(SunkenLockboxEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();

        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);

            poseStack.pushPose();

            // Смещаем модель с учетом того, что это мультиблок 2x1,
            // а центр координат модели в Blockbench смещен вбок (из-за габаритов 23 пикселей в ширину)
            switch (facing) {
                case NORTH -> poseStack.translate(-1.0, 0, 0.0);
                case SOUTH -> poseStack.translate(1.0, 0, 0.0);
                case WEST  -> poseStack.translate(0.0, 0, 1.0);
                case EAST  -> poseStack.translate(0.0, 0, -1.0);
            }

            super.render(be, partialTick, poseStack, buffer, light, overlay);
            poseStack.popPose();
            return;
        }

        super.render(be, partialTick, poseStack, buffer, light, overlay);
    }
}