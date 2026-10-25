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
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {

    /** Первый контрольный пункт кривой под водой (1 = до самой поверхности). */
    private static final float DF_P1 = 1.0f;
    /** Как далеко по направлению к руке лежит второй контрольный пункт (в покое / при полном натяге). */
    private static final float DF_P2_REACH_SLACK = 0.45f;
    private static final float DF_P2_REACH_TAUT  = 0.75f;

    /** Вибрация струны: амплитуда (блоки) = LIN * taut + QUAD * taut², частота в рад/мс. */
    private static final float DF_VIB_LIN  = 0.030f;
    private static final float DF_VIB_QUAD = 0.080f;
    private static final float DF_VIB_FREQ = 0.036f;   // ≈ 5,7 Гц
    private static final float DF_ATTACH = 0.05f;
    private static boolean dfUnder;
    private static float dfTaut;        // натянутость 0..1 для текущей лески
    private static float dfTautSmooth;  // сглаженное значение для лески игрока
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

        // натянутость: только у лески самого игрока и пока идёт бой
        if (hook.getPlayerOwner() == mc.player) {
            long now = net.minecraft.Util.getMillis();
            float dt = dfLastMs == 0 ? 0.016f : Math.min(0.1f, (now - dfLastMs) / 1000f);
            dfLastMs = now;

            float raw = FightClient.active ? Mth.clamp(FightClient.tension, 0f, 1f) : 0f;
            // мёртвая зона: до 0.35 форма не меняется, к 0.9 леска полностью выпрямлена
            float target = dfSmoothstep(0.35f, 0.9f, raw);
            // нарастает за ~0,6 с, отпускает ещoe медленнее (~1 с)
            float rate = target > dfTautSmooth ? 1.6f : 1.0f;
            dfTautSmooth += (target - dfTautSmooth) * Math.min(1f, rate * dt);
            if (dfTautSmooth < 0.005f) dfTautSmooth = 0f;
            dfTaut = dfTautSmooth;
        } else {
            dfTaut = 0f;
        }

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

    private static float[] dfPoint(float x, float y, float z, float u) {
        float taut = dfTaut;
        float px, py, pz;

        if (!dfUnder) {
            // ванильная кривая с провисанием; при натяге выпрямляется до прямой
            float sag = (u * u + u) * 0.5f;
            float k = sag + (u - sag) * taut;
            px = x * u;
            py = y * k + 0.25f;
            pz = z * u;
        } else {
            // под водой: Безье крючок → поверхность → рука; при натяге леска выпрямляется
            float lift  = 0.05f + 0.85f * taut;
            float reach = DF_P2_REACH_SLACK + (DF_P2_REACH_TAUT - DF_P2_REACH_SLACK) * taut;

            float p1y = dfSurfRel * DF_P1;
            float p2x = x * reach, p2z = z * reach;
            float p2y = dfSurfRel + (y - dfSurfRel) * lift;

            float v = 1f - u;
            float b1 = 3f * v * v * u;
            float b2 = 3f * v * u * u;
            float b3 = u * u * u;

            px = b2 * p2x + b3 * x;
            py = b1 * p1y + b2 * p2y + b3 * y + 0.25f;   // +0.25: тот же сдвиг, что в ванили
            pz = b2 * p2z + b3 * z;
        }

        if (taut > 0.01f) {
            // вибрация струны: стоячая волна, концы закреплены, ровная частота (без случайного шума)
            float amp = DF_VIB_LIN * taut + DF_VIB_QUAD * taut * taut;
            float w = net.minecraft.Util.getMillis() * DF_VIB_FREQ;
            float env1 = Mth.sin(u * (float) Math.PI);
            float env2 = Mth.sin(u * 2f * (float) Math.PI);
            float wave = env1 * Mth.sin(w) + 0.35f * env2 * Mth.sin(w * 1.7f + 1.0f);

            // колебания поперёк лески: горизонтальный перпендикуляр и вертикаль
            float hl = Mth.sqrt(x * x + z * z);
            float hx = hl > 1.0E-4f ? -z / hl : 0f;
            float hz = hl > 1.0E-4f ?  x / hl : 0f;
            float wave2 = env1 * Mth.cos(w * 0.9f + 0.5f);

            py += wave * amp;
            px += hx * wave2 * amp * 0.7f;
            pz += hz * wave2 * amp * 0.7f;
        }
        // начало лески чуть утоплено в поплавок, смещение гаснет за первую шестую часть лески
        float a = Math.max(0f, 1f - u * 6f);
        py -= DF_ATTACH * a * a;
        // страховка: леска никогда не уходит выше руки/поверхности (и NaN не пропускаем)
        float maxY = Math.max(y, dfUnder ? dfSurfRel : y) + 0.25f + 0.05f;
        if (!Float.isFinite(py) || py > maxY) py = Math.min(maxY, Float.isFinite(py) ? py : maxY);
        if (!Float.isFinite(px)) px = x * u;
        if (!Float.isFinite(pz)) pz = z * u;
        return new float[]{px, py, pz};
    }
}