package ru.imaginaerum.damagecore.mixin.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.fishing.client.DfLineState;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {

    private static final float DF_BOBBER_RAISE = 0.12f;
    private static float dfRaise;   // текущий подъём поплавка, 0 если нет боя
    private static final float DF_SAG_DEPTH = 1.6f;    // было 0.9
    private static final float DF_VIB_LIN   = 0.006f;
    private static final float DF_VIB_QUAD  = 0.030f;  // дрожь заметно растёт только у предела

    private static final float DF_VIB_FREQ = 0.075f;   // было 0.030, ≈ 12 Гц
    private static float dfTautSmooth;                 // сглаженная дрожь 0..1
    private static float dfTautUse;                    // применяется к текущей леске
    /** Вибрация струны: амплитуда (блоки) = LIN * taut + QUAD * taut², частота в рад/мс. */
    private static float dfSwayAmt;     // 1 в покое, 0 в бою
    private static float dfSwayPhase;
    private static final float DF_ATTACH = 0.05f;
    private static boolean dfUnder;
    private static float dfTaut;
    /** Провис лески в покое (0..1 от DF_SAG_DEPTH); при выдохшейся рыбе будет глубже. */
    private static final float DF_SAG_IDLE = 0.5f;
    private static float dfSlackUse;   // значение, которое реально применяется к текущей леске// натянутость 0..1 для текущей лески
    /** Максимальная стрела прогиба лески (блоки) при полном провисании. */
    private static float dfSurfRel;     // высота поверхности относительно точки привязки лески
    private static long dfLastMs;
    @Shadow
    private static void stringVertex(float x, float y, float z, VertexConsumer consumer,
                                     PoseStack.Pose pose, float frac, float nextFrac) {
        throw new AssertionError();
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void deepfishing$head(FishingHook hook, float yaw, float pt, PoseStack ps,
                                  MultiBufferSource buf, int light, CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        long now = net.minecraft.Util.getMillis();
        DfLineState ln = DfLineState.get(hook.getId(), now);
        float dt = ln.ms == 0 ? 0.016f : Math.min(0.1f, (now - ln.ms) / 1000f);
        ln.ms = now;

        boolean mine = hook.getPlayerOwner() == mc.player;
        boolean landedNow = hook.isInWater() || hook.onGround() || hook.getHookedIn() != null;
        if (landedNow) ln.landed = true;
        boolean landed = ln.landed;

        float targetSlack;
        float rate;
        if (mine && FightClient.active) {
            targetSlack = 1f - dfSmoothstep(0.05f, 0.80f, Mth.clamp(FightClient.tension, 0f, 1f));
            rate = targetSlack < ln.slack ? 2.7f : 1.0f;   // было 8 и 3: втрое медленнее
        } else if (landed) {
            targetSlack = DF_SAG_IDLE;
            rate = 1.2f;            // после приводнения леска оседает медленно, ~1,5 с
        } else {
            targetSlack = 0f;       // пока крючок летит, леска прямая
            rate = 6f;
        }
        ln.slack += (targetSlack - ln.slack) * Math.min(1f, rate * dt);
        if (ln.slack < 0.005f) ln.slack = 0f;
        dfSlackUse = ln.slack;
        dfSwayAmt = (mine && FightClient.active) ? 0f : 1f;
        dfSwayPhase = now * 0.0018f + hook.getId() * 1.7f;   // у каждого крючка своя фаза
        if (mine) {
            float rawTaut = FightClient.active
                    ? dfSmoothstep(0.75f, 1.0f, Mth.clamp(FightClient.tension, 0f, 1f)) * (1f - ln.slack)
                    : 0f;
            float tRate = rawTaut > dfTautSmooth ? 6f : 3f;
            dfTautSmooth += (rawTaut - dfTautSmooth) * Math.min(1f, tRate * dt);
            if (dfTautSmooth < 0.005f) dfTautSmooth = 0f;
            dfTautUse = dfTautSmooth;
            dfRaise = FightClient.active ? DF_BOBBER_RAISE : 0f;
        } else {
            dfTautUse = 0f;
            dfRaise = 0f;
        }
        if (dfRaise > 0f) ps.translate(0.0, dfRaise, 0.0);
        dfUnder = false;
        Level level = hook.level();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(hook.getX(), hook.getY(), hook.getZ());
        if (!level.getFluidState(p).is(FluidTags.WATER)) {
            p.move(0, -1, 0);
            if (!level.getFluidState(p).is(FluidTags.WATER)) return;
        }

        for (int i = 0; i < 64 && level.getFluidState(p.above()).is(FluidTags.WATER); i++) {
            p.move(0, 1, 0);
        }
        FluidState top = level.getFluidState(p);
        double surfaceY = p.getY() + top.getHeight(level, p) + 0.02;

        // та же точка, что в ванили: lerp(hook.y) + 0.25
        double originY = Mth.lerp(pt, hook.yo, hook.getY()) + 0.25;
        float rel = (float) (surfaceY - originY);
        dfUnder = true;
        dfSurfRel = Math.max(rel, 0.15f);   // у поверхности держим минимальный вертикальный участок, иначе изгиб дёргается от покачивания поплавка
    }
    private static float dfSmoothstep(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void deepfishing$tail(FishingHook hook, float yaw, float pt, PoseStack ps,
                                  MultiBufferSource buf, int light, CallbackInfo ci) {
        if (dfRaise > 0f) ps.translate(0.0, -dfRaise, 0.0);
        dfRaise = 0f;
        dfUnder = false;
        dfTaut = 0f;
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FF)V"))
    private static void deepfishing$string(float x, float y, float z, VertexConsumer consumer,
                                           PoseStack.Pose pose, float frac, float nextFrac) {
        float[] a = dfPoint(x, y, z, frac);
        float[] b = dfPoint(x, y, z, nextFrac);
        float nx = b[0] - a[0], ny = b[1] - a[1], nz = b[2] - a[2];
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0E-6f) { nx /= len; ny /= len; nz /= len; }
        consumer.addVertex(pose, a[0], a[1], a[2]).setColor(-16777216).setNormal(pose, nx, ny, nz);
    }
    private static final float DF_STICK = 0.05f;
    private static float[] dfPoint(float x, float y, float z, float u) {
        float px = x * u;
        float py = y * u + 0.25f - dfRaise * u - DF_STICK * (1f - u);
        float pz = z * u;

        // провис: парабола с нулевым прогибом на концах, вниз
        if (dfSlackUse > 0f) {
            float len = Mth.sqrt(x * x + y * y + z * z);
            float sag = Mth.sin(dfSwayPhase) * 0.10f * dfSwayAmt;          // глубина чуть «дышит»
            float depth = DF_SAG_DEPTH * dfSlackUse * (1f + sag) * Mth.clamp(len / 4f, 0.35f, 1f);
            float env = 4f * u * (1f - u);                                   // концы неподвижны
            py -= depth * env;

            // лёгкое боковое покачивание
            float hl = Mth.sqrt(x * x + z * z);
            if (hl > 1.0E-4f) {
                float side = Mth.sin(dfSwayPhase * 0.8f + 1.0f) * 0.025f * dfSwayAmt * dfSlackUse * env;
                px += (-z / hl) * side;
                pz += ( x / hl) * side;
            }
        }
        // дрожь натянутой струны: у концов амплитуда нулевая и с нулевым наклоном, леска там остаётся прямой
        float taut = dfTautUse;
        if (taut > 0.01f) {
            float amp = DF_VIB_LIN * taut + DF_VIB_QUAD * taut * taut;
            float w = net.minecraft.Util.getMillis() * DF_VIB_FREQ;

            float fade = dfSmoothstep(0f, 0.3f, u) * dfSmoothstep(0f, 0.15f, 1f - u);
            float env1 = Mth.sin(u * (float) Math.PI) * fade;
            float env2 = Mth.sin(u * 2f * (float) Math.PI) * fade;
            float wave = env1 * Mth.sin(w) + 0.08f * env2 * Mth.sin(w * 1.7f + 1.0f);

            float hl = Mth.sqrt(x * x + z * z);
            float hx = hl > 1.0E-4f ? -z / hl : 0f;
            float hz = hl > 1.0E-4f ?  x / hl : 0f;
            float wave2 = env1 * Mth.cos(w * 0.9f + 0.5f);

            py += wave * amp;
            px += hx * wave2 * amp * 0.7f;
            pz += hz * wave2 * amp * 0.7f;
        }
        return new float[]{px, py, pz};
    }
}