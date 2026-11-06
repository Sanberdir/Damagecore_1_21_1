package ru.imaginaerum.damagecore.mixin.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    @Unique private static final float DF_SAG_DELAY = 22f;
    @Unique private static float df$straight;
    @Unique private static final float DF_BOBBER_DY = -0.05f;
    @Unique private static final float DF_TENSION_SLACK = 0.30f;
    @Unique private static final float DF_TENSION_TAUT  = 0.55f;
    @Unique private static final float DF_SMOOTH = 0.2f;
    @Unique private static float df$own = 0f;
    @Unique private static float df$shake = 0f;
    @Unique private static final float DF_SHAKE_START = 0.30f;
    @Unique private static final float DF_SHAKE_FULL  = 0.95f;
    @Unique private static final float DF_SHAKE_AMP_MIN = 0.013f;
    @Unique private static final float DF_SHAKE_AMP_MAX = 0.057f;
    @Unique private static final float DF_SHAKE_FREQ_MIN = 1.2f;
    @Unique private static final float DF_SHAKE_FREQ_MAX = 3.4f;
    @Unique private static float df$time = 0f;
    @Unique private static final float DF_SLACK_DEPTH = 0.05f;
    @Unique private static final float DF_SLACK_MAX   = 0.45f;
    @Unique private static float df$phase = 0f;
    @Unique private static float df$lastTime = 0f;
    @Unique private static float df$level = 0f;
    @Unique private static float df$curShake = 0f;
    @Unique private static float df$slack = 0f;

    // Смещение кончика — теперь берётся напрямую из ItemRendererMixin,
    // который считает его из реальной матрицы модели (в camera-space).
    @Unique private static float df$tipX, df$tipY, df$tipZ;

    @Unique private static float df$sagScale = 1f;
    @Unique private static final int DF_POST_FIGHT_SAG_TICKS = 60;
    @Unique private static int df$postFightTicks = 0;
    @Unique private static int df$postFightHookId = -1;
    @Unique private static boolean df$wasActive = false;

    @Unique private static final float DF_TIRED_SAG_DIV = 1.5f;
    @Unique private static float df$tired = 0f;
    @Unique private static float df$tiredScale = 1f;

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void df$begin(FishingHook hook, float yaw, float pt, PoseStack ps,
                          MultiBufferSource buf, int light, CallbackInfo ci) {
        float t = Mth.clamp((hook.tickCount + pt) / DF_SAG_DELAY, 0f, 1f);
        float k = 1f - t * t * (3f - 2f * t);

        Minecraft mc = Minecraft.getInstance();
        boolean own = mc.player != null && hook.getPlayerOwner() == mc.player;
        float shakeTarget = 0f;
        float slackTarget = 0f;
        float levelTarget = 0f;
        if (own) {
            float target = 0f;
            if (FightClient.active) {
                float ts = FightClient.tensionShown;
                float s = Mth.clamp((ts - DF_TENSION_SLACK) / (DF_TENSION_TAUT - DF_TENSION_SLACK), 0f, 1f);
                target = s * s * (3f - 2f * s);

                levelTarget = Mth.clamp((ts - DF_SHAKE_START) / (DF_SHAKE_FULL - DF_SHAKE_START), 0f, 1f);
                shakeTarget = 1f;

                float sl = Mth.clamp((0.50f - ts) / 0.25f, 0f, 1f);
                slackTarget = sl * sl * (3f - 2f * sl);
            }
            df$own += (target - df$own) * DF_SMOOTH;
            if (df$own < 0.001f) df$own = 0f;
            k = Math.max(k, df$own);

            df$shake += (shakeTarget - df$shake) * 0.25f;
            if (df$shake < 0.001f) df$shake = 0f;
            df$level += (levelTarget - df$level) * 0.2f;
            df$slack += (slackTarget - df$slack) * (slackTarget > df$slack ? 0.10f : 0.35f);
            if (df$slack < 0.001f) df$slack = 0f;
        }
        df$curShake = own ? df$shake * (levelTarget > 0f || df$level > 0.01f ? 1f : 0f) : 0f;

        // Смещение кончика берём напрямую из ItemRendererMixin.
        // Оно уже в camera-space — том же пространстве, в котором
        // FishingHookRenderer строит струну. Никаких cam.getLookVector() не нужно.
        if (own && mc.options.getCameraType().isFirstPerson()) {
            df$tipX = FightClient.tipDx;
            df$tipY = FightClient.tipDy;
            df$tipZ = FightClient.tipDz;
        } else {
            df$tipX = df$tipY = df$tipZ = 0f;
        }

        float now = hook.tickCount + pt;
        float dt = now - df$lastTime;
        if (dt < 0f || dt > 3f) dt = 0f;
        df$lastTime = now;
        df$time = now;
        float freq = Mth.lerp(df$level, DF_SHAKE_FREQ_MIN, DF_SHAKE_FREQ_MAX);
        df$phase += freq * dt;

        df$straight = k;

        boolean nowActive = own && FightClient.active;
        if (df$wasActive && !nowActive) {
            df$postFightTicks = DF_POST_FIGHT_SAG_TICKS;
            df$postFightHookId = hook.getId();
        }
        df$wasActive = nowActive;
        if (df$postFightTicks > 0) df$postFightTicks--;
        float postFight = (own && df$postFightTicks > 0 && hook.getId() == df$postFightHookId) ? 0.5f : 1f;

        float tiredTarget = (own && FightClient.active && FightClient.dash == 3) ? 1f : 0f;
        df$tired += (tiredTarget - df$tired) * 0.1f;
        if (df$tired < 0.001f) df$tired = 0f;
        df$tiredScale = Mth.lerp(df$tired, 1f, 1f / DF_TIRED_SAG_DIV);

        df$sagScale = postFight * df$tiredScale;
    }

    @Inject(method = "stringVertex", at = @At("HEAD"), cancellable = true)
    private static void df$string(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose,
                                  float t0, float t1, CallbackInfo ci) {
        float k = df$straight;

        // t — параметр вдоль струны от крючка (0) к кончику удочки (1).
        // Смещение кончика должно НАРАСТАТЬ к кончику, а не гаснуть —
        // поэтому вес = t, а не (1 - t).
        float a0 = t0, a1 = t1;

        float w0 = (1f - t0) * (1f - t0) * (1f - t0) * (1f - t0);
        float w1 = (1f - t1) * (1f - t1) * (1f - t1) * (1f - t1);

        float x0 = x * t0, z0 = z * t0;
        float x1 = x * t1, z1 = z * t1;

        float f0 = Mth.lerp(k, (t0 * t0 + t0) * 0.5f, t0);
        float f1 = Mth.lerp(k, (t1 * t1 + t1) * 0.5f, t1);
        f0 = t0 + (f0 - t0) * df$sagScale;
        f1 = t1 + (f1 - t1) * df$sagScale;

        float y0 = y * f0 + 0.25f + DF_BOBBER_DY * w0 * df$sagScale;
        float y1 = y * f1 + 0.25f + DF_BOBBER_DY * w1 * df$sagScale;

        // Смещение кончика: tip уже соответствует реальной позе модели,
        // поэтому НЕ масштабируем его через df$sagScale.
        x0 += df$tipX * a0;  y0 += df$tipY * a0;  z0 += df$tipZ * a0;
        x1 += df$tipX * a1;  y1 += df$tipY * a1;  z1 += df$tipZ * a1;

        if (df$shake > 0f) {
            float ll = Mth.sqrt(x * x + y * y + z * z);
            if (ll > 1.0E-4f) {
                float dxn = x / ll, dyn = y / ll, dzn = z / ll;

                float p1x = dzn, p1z = -dxn;
                float pl = Mth.sqrt(p1x * p1x + p1z * p1z);
                if (pl < 1.0E-4f) { p1x = 1f; p1z = 0f; pl = 1f; }
                p1x /= pl; p1z /= pl;
                float p2x = dyn * p1z;
                float p2y = dzn * p1x - dxn * p1z;
                float p2z = -dyn * p1x;

                float amp = Mth.lerp(df$level, DF_SHAKE_AMP_MIN, DF_SHAKE_AMP_MAX);
                float env = amp * df$curShake * (0.85f + 0.15f * Mth.sin(df$time * 0.35f));
                float c1 = Mth.cos(df$phase);
                float c2 = 0.3f * Mth.sin(df$phase);

                float s0 = df$mode(t0), s1 = df$mode(t1);
                float u0 = env * s0, u1 = env * s1;

                x0 += u0 * (p2x * c1 + p1x * c2);
                y0 += u0 * (p2y * c1);
                z0 += u0 * (p2z * c1 + p1z * c2);
                x1 += u1 * (p2x * c1 + p1x * c2);
                y1 += u1 * (p2y * c1);
                z1 += u1 * (p2z * c1 + p1z * c2);
            }
        }
        if (df$slack > 0f) {
            float L = Mth.sqrt(x * x + z * z);
            float sway = 1f + 0.06f * Mth.sin(df$time * 0.11f);
            float depth = Math.min(L * DF_SLACK_DEPTH, DF_SLACK_MAX) * df$slack * sway * df$tiredScale;
            y0 -= depth * 4f * t0 * (1f - t0);
            y1 -= depth * 4f * t1 * (1f - t1);
        }
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6f) { dx = 0f; dy = 1f; dz = 0f; } else { dx /= len; dy /= len; dz /= len; }

        consumer.addVertex(pose, x0, y0, z0).setColor(-16777216).setNormal(pose, dx, dy, dz);
        ci.cancel();
    }

    @Unique
    private static float df$mode(float t) {
        float m1 = Mth.sin(t * Mth.PI);
        float m2 = Mth.sin(t * Mth.PI * 2f) * 0.25f * Mth.cos(df$phase * 0.5f);
        return m1 + m2;
    }
}