// src/main/java/ru/imaginaerum/damagecore/effect/effects/Drunkenness2Handler.java
package ru.imaginaerum.damagecore.effect.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.effect.DCEffects;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class Drunkenness2Handler {

    // Переменные для хранения предыдущих значений для плавности
    private static float prevWaveX = 0;
    private static float prevWaveY = 0;
    private static float prevWaveZ = 0;
    private static float prevShake = 0;
    private static float prevPixelScale = 1.0f;

    private static float prevCameraShakeX = 0;
    private static float prevCameraShakeY = 0;
    private static float prevCameraRoll = 0;

    private static float swayTimer = 0;
    private static float prevSwayX = 0;
    private static float prevSwayZ = 0;

    // Факторы плавности
    private static final float SMOOTH_FACTOR = 0.25f;
    private static final float CAMERA_SMOOTH_FACTOR = 0.3f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_2)) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_2).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                resetValues();
                return;
            }

            long time = System.currentTimeMillis();

            // Интенсивность
            float intensity = (amplifier + 1) * 0.2f;
            float pixelIntensity = (amplifier + 1) * 0.3f;

            // Вычисляем целевые значения для волн (сохраняем оригинальную скорость)
            float targetWaveX = (float) Math.sin(time * 0.001) * intensity;
            float targetWaveY = (float) Math.cos(time * 0.001 + 100) * intensity * 0.5f;

            // Плавно интерполируем к целевым значениям
            float waveX = lerp(prevWaveX, targetWaveX, SMOOTH_FACTOR);
            float waveY = lerp(prevWaveY, targetWaveY, SMOOTH_FACTOR);

            // Сохраняем для следующего кадра
            prevWaveX = waveX;
            prevWaveY = waveY;

            // Целевое значение дрожания
            float targetShake = (float) Math.sin(time * 0.01) * intensity * 0.15f;

            // Плавное дрожание
            float shake = lerp(prevShake, targetShake, SMOOTH_FACTOR * 1.2f);
            prevShake = shake;

            PoseStack poseStack = event.getPoseStack();

            // Плавная пикселизация
            if (pixelIntensity > 0.1f) {
                // Плавная "тошнотная" пикселизация
                float pixelWave =
                        (float) Math.sin(time * 0.0008f) * 0.5f + 0.5f; // 0..1

                float targetPixelScale =
                        1.0f - pixelIntensity * 0.08f * pixelWave;

                float pixelScale =
                        lerp(prevPixelScale, targetPixelScale, SMOOTH_FACTOR * 0.4f);

                prevPixelScale = pixelScale;
                poseStack.scale(pixelScale, pixelScale, pixelScale);
            } else {
                prevPixelScale = 1.0f;
            }

            // Плавное дрожание масштаба
            poseStack.scale(1.0f + shake, 1.0f + shake, 1.0f);

            // Движение пикселей (наклоны) - используем интерполированные значения
            // Уменьшаем немного множитель для большей плавности
            float tiltX = waveY * 14.0f; // Было 15.0
            float tiltY = waveX * 14.0f;

            poseStack.mulPose(Axis.XP.rotationDegrees(tiltX));
            poseStack.mulPose(Axis.YP.rotationDegrees(tiltY));

            // Плавный наклон по Z
            float targetWaveZ = (float) Math.sin(time * 0.0015 + 50) * intensity * 0.3f;
            float waveZ = lerp(prevWaveZ, targetWaveZ, SMOOTH_FACTOR);
            prevWaveZ = waveZ;

            poseStack.mulPose(Axis.ZP.rotationDegrees(waveZ * 4.5f)); // Было 5.0
        } else {
            // Плавный выход из эффекта
            prevWaveX = lerp(prevWaveX, 0, SMOOTH_FACTOR * 0.5f);
            prevWaveY = lerp(prevWaveY, 0, SMOOTH_FACTOR * 0.5f);
            prevWaveZ = lerp(prevWaveZ, 0, SMOOTH_FACTOR * 0.5f);
            prevShake = lerp(prevShake, 0, SMOOTH_FACTOR * 0.5f);
            prevPixelScale = lerp(prevPixelScale, 1.0f, SMOOTH_FACTOR * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_2)) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_2).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                prevCameraShakeX = 0;
                prevCameraShakeY = 0;
                prevCameraRoll = 0;
                return;
            }

            float intensity = (amplifier + 1) * 0.25f;
            long time = System.currentTimeMillis();

            // Целевые значения дрожания камеры (немного замедляем)
            float targetCameraShakeX = (float) Math.sin(time * 0.018) * intensity * 1.8f; // Было 0.02 * 2.0
            float targetCameraShakeY = (float) Math.cos(time * 0.015) * intensity * 1.3f; // Было 0.017 * 1.5
            float targetCameraRoll = (float) Math.sin(time * 0.014) * intensity * 4.0f;   // Было 0.015 * 5.0

            // Плавная интерполяция камеры
            float cameraShakeX = lerp(prevCameraShakeX, targetCameraShakeX, CAMERA_SMOOTH_FACTOR);
            float cameraShakeY = lerp(prevCameraShakeY, targetCameraShakeY, CAMERA_SMOOTH_FACTOR);
            float cameraRoll = lerp(prevCameraRoll, targetCameraRoll, CAMERA_SMOOTH_FACTOR);

            // Сохраняем
            prevCameraShakeX = cameraShakeX;
            prevCameraShakeY = cameraShakeY;
            prevCameraRoll = cameraRoll;

            event.setYaw(event.getYaw() + cameraShakeX);
            event.setPitch(event.getPitch() + cameraShakeY);
            event.setRoll(event.getRoll() + cameraRoll);
        } else {
            // Плавный сброс камеры
            prevCameraShakeX = lerp(prevCameraShakeX, 0, CAMERA_SMOOTH_FACTOR);
            prevCameraShakeY = lerp(prevCameraShakeY, 0, CAMERA_SMOOTH_FACTOR);
            prevCameraRoll = lerp(prevCameraRoll, 0, CAMERA_SMOOTH_FACTOR);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        if (player.hasEffect(DCEffects.DRUNKENNESS_2)) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_2).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                swayTimer = 0;
                prevSwayX = 0;
                prevSwayZ = 0;
                return;
            }

            float intensity = (amplifier + 1) * 0.15f;

            // Увеличиваем таймер (немного медленнее)
            swayTimer += 0.085f;

            // Целевые значения шатания (немного уменьшаем амплитуду)
            float targetSwayX = Mth.sin(swayTimer * 0.45f) * intensity * 0.55f;
            float targetSwayZ = Mth.cos(swayTimer * 0.28f) * intensity * 0.55f;

            // Плавное шатание
            float swayX = lerp(prevSwayX, targetSwayX, 0.2f);
            float swayZ = lerp(prevSwayZ, targetSwayZ, 0.2f);

            prevSwayX = swayX;
            prevSwayZ = swayZ;

            if (player.onGround() && (player.xxa != 0 || player.zza != 0)) {
                player.setPos(
                        player.getX() + swayX * 0.8f,
                        player.getY(),
                        player.getZ() + swayZ * 0.8f
                );
            }
        } else {
            swayTimer = 0;
            prevSwayX = lerp(prevSwayX, 0, 0.15f);
            prevSwayZ = lerp(prevSwayZ, 0, 0.15f);
        }
    }

    // Функция линейной интерполяции
    private static float lerp(float start, float end, float amount) {
        amount = Math.max(0, Math.min(1, amount));
        return start + (end - start) * amount;
    }

    // Сброс всех значений
    private static void resetValues() {
        prevWaveX = 0;
        prevWaveY = 0;
        prevWaveZ = 0;
        prevShake = 0;
        prevPixelScale = 1.0f;
        prevCameraShakeX = 0;
        prevCameraShakeY = 0;
        prevCameraRoll = 0;
        swayTimer = 0;
        prevSwayX = 0;
        prevSwayZ = 0;
    }
}