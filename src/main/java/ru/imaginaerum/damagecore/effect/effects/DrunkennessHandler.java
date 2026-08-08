// src/main/java/ru/imaginaerum/damagecore/effect/effects/DrunkennessHandler.java
package ru.imaginaerum.damagecore.effect.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.effect.DCEffects;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class DrunkennessHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_1)) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_1).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                return;
            }

            long time = System.currentTimeMillis();

            float intensity = (amplifier + 1) * 0.15f;

            // ЗАМЕДЛЕННЫЕ ЧАСТОТЫ:
            // Волна: 0.001 → 0.0007 (в 1.43 раза медленнее)
            float waveX = (float) Math.sin(time * 0.0007) * intensity;
            float waveY = (float) Math.cos(time * 0.0007 + 100) * intensity * 0.5f;

            // Дрожание: 0.01 → 0.007 (в 1.43 раза медленнее)
            float shake = (float) Math.sin(time * 0.007) * intensity * 0.1f;

            PoseStack poseStack = event.getPoseStack();

            poseStack.scale(1.0f + shake, 1.0f + shake, 1.0f);

            // Наклон - немного уменьшил множитель для плавности
            poseStack.mulPose(Axis.XP.rotationDegrees(waveY * 9.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(waveX * 9.0f));
        }
    }
}