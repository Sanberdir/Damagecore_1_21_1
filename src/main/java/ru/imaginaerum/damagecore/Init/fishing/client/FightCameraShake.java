package ru.imaginaerum.damagecore.Init.fishing.client;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public final class FightCameraShake {
    static final float DEAD_LOW  = 0.35f;  // ниже этого натяжения камера не дрожит
    static final float DEAD_HIGH = 0.90f;  // выше этого дрожь максимальная
    static final float CAM_DEG = 0.08f;
    static final float RISE      = 2.0f;   // скорость нарастания (1/с)
    static final float FALL      = 1.5f;   // скорость затухания (1/с)

    private static float smooth;           // сглаженная сила 0..1
    private static long lastMs;

    private FightCameraShake() {}

    private static float smoothstep(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = net.minecraft.Util.getMillis();
        float dt = lastMs == 0 ? 0.016f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;

        float raw = FightClient.active ? Mth.clamp(FightClient.tension, 0f, 1f) : 0f;
        float target = smoothstep(DEAD_LOW, DEAD_HIGH, raw);
        float rate = target > smooth ? RISE : FALL;
        smooth += (target - smooth) * Math.min(1f, rate * dt);



        // вибрация натяга: ~12 Гц + лёгкие биения, мягкая за счёт малой амплитуды
        float t = (now % 100000L) * 0.001f;
        float amp = CAM_DEG * smooth;
        float beat = 0.75f + 0.25f * Mth.sin(t * 5.0f);   // медленное дыхание силы, чтобы не было «моторчика»

        float tx = (Mth.sin(t * 75f) + 0.4f * Mth.sin(t * 121f + 1.3f)) * amp * beat;
        float ty = (Mth.sin(t * 68f + 2.1f) + 0.4f * Mth.sin(t * 109f + 0.4f)) * amp * 0.8f * beat;
        float tr = Mth.sin(t * 57f + 0.8f) * amp * 0.5f * beat;

        event.setPitch(event.getPitch() + tx);
        event.setYaw(event.getYaw() + ty);
        event.setRoll(event.getRoll() + tr);
    }
}