package ru.imaginaerum.damagecore.mixin.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;
import ru.imaginaerum.damagecore.Init.fishing.client.BendConsumer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique
    private static boolean df$active;
    @Unique private static final float DF_ROLL_DEG   = 110f;
    @Unique private static final float DF_PITCH_DEG  = 150f;
    @Unique private static final float DF_SLIDE_SIDE = 0.06f;
    @Unique private static final float DF_SLIDE_UP   = 0.08f;
    @Unique private static final float DF_BASE_SIDE = 1.0f;
    @Unique private static final float DF_BASE_FWD  = 1.0f;
    @Unique private static final float DF_YAW_DEG   = 100f;
    @Unique private static final float DF_SLIDE_FWD  = 0.05f;
    @Unique private static final Matrix4f df$B  = new Matrix4f();
    @Unique private static final Matrix4f df$BE = new Matrix4f();

    @Inject(method = "render", at = @At("HEAD"))
    private void df$begin(ItemStack stack, ItemDisplayContext ctx, boolean left, PoseStack ps,
                          MultiBufferSource buf, int light, int overlay, BakedModel model, CallbackInfo ci) {

        df$sample();
        boolean isRod       = stack.getItem() instanceof FishingRodItem;
        boolean firstPerson = ctx.firstPerson();
        boolean bent        = Math.abs(df$bend)  > 0.002f || Math.abs(df$side)  > 0.002f
                || Math.abs(df$fwd)   > 0.002f || Math.abs(df$bside) > 0.002f;

        df$active = firstPerson && isRod && bent;

        if (df$active) {
            df$B.set(ps.last().pose());
            ps.pushPose();
            ps.translate(df$bside * DF_SLIDE_SIDE, df$fwd * DF_SLIDE_UP, 0.0);
            ps.mulPose(Axis.ZP.rotationDegrees(-df$bside * DF_ROLL_DEG));
            ps.mulPose(Axis.XP.rotationDegrees(df$fwd * DF_PITCH_DEG));
            df$BE.set(ps.last().pose());
        } else if (firstPerson && isRod) {
            // удочка в руке, но не согнута — сбрасываем прошлое смещение кончика
            FightClient.tipDx = FightClient.tipDy = FightClient.tipDz = 0f;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void df$end(ItemStack stack, ItemDisplayContext ctx, boolean left, PoseStack ps,
                        MultiBufferSource buf, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (df$active) ps.popPose();
        df$active = false;
    }

    @Inject(method = "renderModelLists", at = @At("HEAD"))
    private void df$tip(BakedModel model, ItemStack stack, int light, int overlay,
                        PoseStack ps, VertexConsumer c, CallbackInfo ci) {
        if (!df$active) return;

        float tu = 0.94f, tv = 0.94f;
        float off  = BendConsumer.offsetAt(df$bend, tu, tv);
        float offZ = BendConsumer.sideAt(df$side, tu, tv);

        Matrix4f m = ps.last().pose();
        Matrix4f mw = new Matrix4f(df$B).mul(new Matrix4f(df$BE).invert()).mul(m);

        org.joml.Vector3f p0 = mw.transformPosition(tu, tv, 0.5f, new org.joml.Vector3f());
        org.joml.Vector3f p1 = m.transformPosition(tu + off, tv - off, 0.5f + offZ, new org.joml.Vector3f());
        p1.sub(p0);

        FightClient.tipDx = p1.x; FightClient.tipDy = p1.y; FightClient.tipDz = p1.z;
    }

    @ModifyVariable(method = "renderModelLists", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer df$wrap(VertexConsumer c) {
        return df$active ? new BendConsumer(c, df$bend, df$side) : c;
    }
    @Unique private static float df$bend, df$side, df$fwd, df$bside;

    @Unique
    private static void df$sample() {
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        df$bend  = FightClient.bendAt(pt);
        df$side  = FightClient.sideAt(pt);
        df$fwd   = FightClient.fwdAt(pt);
        df$bside = FightClient.baseSideAt(pt);
    }
}