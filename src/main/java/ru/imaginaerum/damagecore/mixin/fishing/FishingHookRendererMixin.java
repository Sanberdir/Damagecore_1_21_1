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
    /** Сколько тиков после заброса леска остаётся прямой, а потом плавно провисает. */
    @Unique private static final float DF_SAG_DELAY = 22f;
    /** 1 = леска прямая, 0 = ванильный провис. */
    @Unique private static float df$straight;
    @Unique private static final float DF_BOBBER_DY = -0.05f;
    @Unique private static final float DF_TENSION_SLACK = 0.30f;
    @Unique private static final float DF_TENSION_TAUT  = 0.55f;
    /** Плавность перехода между прямой и провисшей (0..1 за кадр). */
    @Unique private static final float DF_SMOOTH = 0.2f;
    @Unique private static float df$own = 0f;
    /** Сила дрожи лески 0..1 (сглаженная). */
    @Unique private static float df$shake = 0f;
    /** Время в тиках с долей, для фазы колебаний. */
    @Unique private static final float DF_SHAKE_START = 0.30f;  // с какого натяга начинается дрожь
    @Unique private static final float DF_SHAKE_FULL  = 0.95f;  // максимум
    @Unique private static final float DF_SHAKE_AMP_MIN = 0.013f;  // было 0.020f
    @Unique private static final float DF_SHAKE_AMP_MAX = 0.057f;  // было 0.085f
    @Unique private static final float DF_SHAKE_FREQ_MIN = 1.2f;  // рад/тик (~4 Гц)
    @Unique private static final float DF_SHAKE_FREQ_MAX = 3.4f;  // рад/тик (~11 Гц)
    @Unique private static float df$time = 0f;
    @Unique private static final float DF_SHAKE_AMP  = 0.022f;  // макс. отклонение середины, блоков
    @Unique private static final float DF_SHAKE_FREQ = 4.2f;
    /** Глубина провиса как доля горизонтальной длины лески. */
    @Unique private static final float DF_SLACK_DEPTH = 0.05f;  // было 0.20
    @Unique private static final float DF_SLACK_MAX   = 0.45f;  // было 1.8
    /** Текущая степень слабины 0..1 (сглаженная). */
    @Unique private static float df$phase = 0f;     // накопленная фаза
    @Unique private static float df$lastTime = 0f;
    @Unique private static float df$level = 0f;     // 0..1, сглаженный уровень натяга для дрожи
    @Unique private static float df$curShake = 0f;  // значение для текущей отрисовки (0 для чужих крючков)
    @Unique private static float df$slack = 0f;
    @Unique private static float df$tipX, df$tipY, df$tipZ;
    @Unique private static final float DF_TIP_FWD  = 0.40f;
    @Unique private static final float DF_TIP_DOWN = 0.28f;   // меньше, чем FWD
    @Unique private static final float DF_TIP_SIDE = 0.35f;   // смещение кончика вбок, подбери на глаз
    /** Смещение кончика в блоках на 1.0 rodBend (масштаб модели в руке ~0.68 * 0.7071). Подбери на глаз. */
    @Unique private static final float DF_TIP_SCALE = 0.48f;
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

            // сглаживаем только для своей лески
            df$shake += (shakeTarget - df$shake) * 0.25f;
            if (df$shake < 0.001f) df$shake = 0f;
            df$level += (levelTarget - df$level) * 0.2f;
            df$slack += (slackTarget - df$slack) * (slackTarget > df$slack ? 0.10f : 0.35f);
            if (df$slack < 0.001f) df$slack = 0f;
        }
        df$curShake = own ? df$shake * (levelTarget > 0f || df$level > 0.01f ? 1f : 0f) : 0f;
        df$tipX = df$tipY = df$tipZ = 0f;
        if (own && mc.options.getCameraType().isFirstPerson()) {
            df$tipX = FightClient.tipDx; df$tipY = FightClient.tipDy; df$tipZ = FightClient.tipDz;
        }
        if (own && mc.options.getCameraType().isFirstPerson()
                && (Math.abs(FightClient.rodBend) > 0.002f || Math.abs(FightClient.rodSide) > 0.002f)) {
            var cam = mc.gameRenderer.getMainCamera();
            org.joml.Vector3f look = cam.getLookVector();
            org.joml.Vector3f up   = cam.getUpVector();
            org.joml.Vector3f left = cam.getLeftVector();
            float b = FightClient.rodBend;
            float s = FightClient.rodSide;   // + = вправо
            df$tipX = look.x * b * DF_TIP_FWD - up.x * b * DF_TIP_DOWN - left.x * s * DF_TIP_SIDE;
            df$tipY = look.y * b * DF_TIP_FWD - up.y * b * DF_TIP_DOWN - left.y * s * DF_TIP_SIDE;
            df$tipZ = look.z * b * DF_TIP_FWD - up.z * b * DF_TIP_DOWN - left.z * s * DF_TIP_SIDE;
        }
// накапливаем фазу: чем больше натяг, тем чаще колебания
        float now = hook.tickCount + pt;
        float dt = now - df$lastTime;
        if (dt < 0f || dt > 3f) dt = 0f;
        df$lastTime = now;
        df$time = now;
        float freq = Mth.lerp(df$level, DF_SHAKE_FREQ_MIN, DF_SHAKE_FREQ_MAX);
        df$phase += freq * dt;

        df$straight = k;
    }

    @Inject(method = "stringVertex", at = @At("HEAD"), cancellable = true)
    private static void df$string(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose,
                                  float t0, float t1, CallbackInfo ci) {
        float k = df$straight;

        float a0 = 1f - t0, a1 = 1f - t1;
        float w0 = a0 * a0 * a0 * a0;
        float w1 = a1 * a1 * a1 * a1;

        float x0 = x * t0, z0 = z * t0;
        float x1 = x * t1, z1 = z * t1;

        float y0 = y * Mth.lerp(k, (t0 * t0 + t0) * 0.5f, t0) + 0.25f + DF_BOBBER_DY * w0;
        float y1 = y * Mth.lerp(k, (t1 * t1 + t1) * 0.5f, t1) + 0.25f + DF_BOBBER_DY * w1;
        x0 += df$tipX * t0;  y0 += df$tipY * t0;  z0 += df$tipZ * t0;
        x1 += df$tipX * t1;  y1 += df$tipY * t1;  z1 += df$tipZ * t1;
        if (df$shake > 0f) {
            // направление лески
            float ll = Mth.sqrt(x * x + y * y + z * z);
            if (ll > 1.0E-4f) {
                float dxn = x / ll, dyn = y / ll, dzn = z / ll;

                // p1 = горизонтальный перпендикуляр (d × up), p2 = перпендикуляр к d и p1 (почти вертикаль)
                float p1x = dzn, p1z = -dxn;
                float pl = Mth.sqrt(p1x * p1x + p1z * p1z);
                if (pl < 1.0E-4f) { p1x = 1f; p1z = 0f; pl = 1f; }
                p1x /= pl; p1z /= pl;
                float p2x = dyn * p1z;
                float p2y = dzn * p1x - dxn * p1z;
                float p2z = -dyn * p1x;

                // колебание: преимущественно в одной плоскости, лёгкая эллиптичность,
                // медленная модуляция амплитуды (как затухающий щипок)
                float amp = Mth.lerp(df$level, DF_SHAKE_AMP_MIN, DF_SHAKE_AMP_MAX);
                float env = amp * df$curShake * (0.85f + 0.15f * Mth.sin(df$time * 0.35f));
                float c1 = Mth.cos(df$phase);          // основная плоскость (вертикальная)
                float c2 = 0.3f * Mth.sin(df$phase);   // слабая поперечная составляющая

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
            // лёгкое покачивание провисшей лески, чтобы она выглядела живой
            float sway = 1f + 0.06f * Mth.sin(df$time * 0.11f);
            float depth = Math.min(L * DF_SLACK_DEPTH, DF_SLACK_MAX) * df$slack * sway;
            y0 -= depth * 4f * t0 * (1f - t0);
            y1 -= depth * 4f * t1 * (1f - t1);
        }
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6f) { dx = 0f; dy = 1f; dz = 0f; } else { dx /= len; dy /= len; dz /= len; }

        consumer.addVertex(pose, x0, y0, z0).setColor(-16777216).setNormal(pose, dx, dy, dz);
        ci.cancel();
    }

    /** Форма струны: основная полуволна + слабая 2-я гармоника (с узлом в центре, колеблется в противофазе). */
    @Unique
    private static float df$mode(float t) {
        float m1 = Mth.sin(t * Mth.PI);
        float m2 = Mth.sin(t * Mth.PI * 2f) * 0.25f * Mth.cos(df$phase * 0.5f);
        return m1 + m2;
    }
}