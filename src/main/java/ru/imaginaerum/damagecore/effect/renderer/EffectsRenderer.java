package ru.imaginaerum.damagecore.effect.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.imaginaerum.damagecore.effect.DCEffects;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class EffectsRenderer implements AutoCloseable {
    final Minecraft minecraft;
    private final MapRenderer mapRenderer;
    private final ResourceManager resourceManager;
    private final RenderBuffers renderBuffers;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;
    private ItemStack itemActivationItem;
    @Nullable
    PostChain postEffect;

    public EffectsRenderer(Minecraft pMinecraft, ItemInHandRenderer pItemInHandRenderer, ResourceManager pResourceManager, RenderBuffers pRenderBuffers) {
        this.minecraft = pMinecraft;
        this.resourceManager = pResourceManager;
        this.mapRenderer = new MapRenderer(pMinecraft.getTextureManager(), pMinecraft.getMapDecorationTextures());
        this.renderBuffers = pRenderBuffers;
        this.postEffect = null;
    }

    private static final ResourceLocation DRUNKENNESS_3 = ResourceLocation.withDefaultNamespace("textures/misc/nausea.png");

    @Override
    public void close() throws Exception {

    }

    public void render(float pPartialTicks, long pNanoTime, boolean pRenderLevel) {
        GuiGraphics guigraphics = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
        if (pRenderLevel && this.minecraft.level != null) {
            this.minecraft.getProfiler().popPush("gui");
            if (this.minecraft.player != null) {
                float f = Mth.lerp(pPartialTicks, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity);
                float f1 = this.minecraft.options.screenEffectScale().get().floatValue();
                if (f > 0.0F && this.minecraft.player.hasEffect(DCEffects.DRUNKENNESS_3) && f1 < 1.0F) {
                    this.renderConfusionOverlay(guigraphics, f * (1.0F - f1));
                }
            }

            if (!this.minecraft.options.hideGui || this.minecraft.screen != null) {
                this.renderItemActivationAnimation(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight(), pPartialTicks);
                this.minecraft.gui.render(guigraphics, this.minecraft.getTimer());
                RenderSystem.clear(256, Minecraft.ON_OSX);
            }

            this.minecraft.getProfiler().pop();
        }
    }

    private void renderItemActivationAnimation(int pWidthsp, int pHeightScaled, float pPartialTicks) {
        if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
            int i = 40 - this.itemActivationTicks;
            float f = ((float) i + pPartialTicks) / 40.0F;
            float f1 = f * f;
            float f2 = f * f1;
            float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
            float f4 = f3 * (float) Math.PI;
            float f5 = this.itemActivationOffX * (float) (pWidthsp / 4);
            float f6 = this.itemActivationOffY * (float) (pHeightScaled / 4);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            PoseStack posestack = new PoseStack();
            posestack.pushPose();
            posestack.translate((float) (pWidthsp / 2) + f5 * Mth.abs(Mth.sin(f4 * 2.0F)), (float) (pHeightScaled / 2) + f6 * Mth.abs(Mth.sin(f4 * 2.0F)), -50.0F);
            float f7 = 50.0F + 175.0F * Mth.sin(f4);
            posestack.scale(f7, -f7, f7);
            posestack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
            posestack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            posestack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            this.minecraft.getItemRenderer().renderStatic(this.itemActivationItem, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, posestack, multibuffersource$buffersource, this.minecraft.level, 0);
            posestack.popPose();
            multibuffersource$buffersource.endBatch();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
        }
    }

    private void renderConfusionOverlay(GuiGraphics pGuiGraphics, float pScalar) {
        int i = pGuiGraphics.guiWidth();
        int j = pGuiGraphics.guiHeight();
        pGuiGraphics.pose().pushPose();
        float f = Mth.lerp(pScalar, 2.0F, 1.0F);
        pGuiGraphics.pose().translate((float) i / 2.0F, (float) j / 2.0F, 0.0F);
        pGuiGraphics.pose().scale(f, f, f);
        pGuiGraphics.pose().translate((float) (-i) / 2.0F, (float) (-j) / 2.0F, 0.0F);
        float f1 = 0.2F * pScalar;
        float f2 = 0.4F * pScalar;
        float f3 = 0.2F * pScalar;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        pGuiGraphics.setColor(f1, f2, f3, 1.0F);
        pGuiGraphics.blit(DRUNKENNESS_3, 0, 0, -90, 0.0F, 0.0F, i, j, i, j);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        pGuiGraphics.pose().popPose();
    }
}