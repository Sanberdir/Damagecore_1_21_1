package ru.imaginaerum.damagecore.Init.fishing.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public final class FightCameraShake {
    static final float BASE     = 0.30f;  // постоянная лёгкая дрожь, пока идёт бой/подмотка
    static final float CAM_DEG  = 0.55f;  // амплитуда камеры (градусы) при силе 1.0
    static final float RISE     = 3.0f;   // скорость нарастания (1/с)
    static final float FALL     = 4.0f;   // скорость затухания (1/с)

    private static float smooth;          // сглаженная сила 0..1
    private static float sx, sy, sr;
    private static long lastMs;

    private FightCameraShake() {}

    /** Сила 0..1: её же использует миксин лески. */
    public static float strength() {
        return smooth;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = net.minecraft.Util.getMillis();
        float dt = lastMs == 0 ? 0.016f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;

        // в бою всегда есть лёгкая дрожь, сверху добавляется натяжение
        float target = FightClient.active
                ? BASE + (1f - BASE) * Mth.clamp(FightClient.tension, 0f, 1f)
                : 0f;
        float k = (target > smooth ? RISE : FALL) * dt;
        smooth += (target - smooth) * Math.min(1f, k);

        if (smooth < 0.01f) { sx = sy = sr = 0f; return; }

        float t = (now % 100000L) * 0.001f;
        float amp = CAM_DEG * smooth;
        float tx = (Mth.sin(t * 5.0f) + 0.5f * Mth.sin(t * 11.0f + 1.3f)) * amp;
        float ty = (Mth.sin(t * 4.3f + 2.1f) + 0.5f * Mth.sin(t * 9.7f + 0.4f)) * amp;
        float tr = Mth.sin(t * 3.1f + 0.8f) * amp * 0.5f;

        float s = Math.min(1f, 10f * dt);   // сглаживание не зависит от FPS
        sx += (tx - sx) * s;
        sy += (ty - sy) * s;
        sr += (tr - sr) * s;

        event.setPitch(event.getPitch() + sx);
        event.setYaw(event.getYaw() + sy);
        event.setRoll(event.getRoll() + sr);
    }
}