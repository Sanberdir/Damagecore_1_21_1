package ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class ClientLightningRenderer {

    private static final List<ClientSegment> ACTIVE_SEGMENTS = new ArrayList<>();

    /**
     * Принимает две точки и флаг красной молнии
     */
    public static void addChainSegment(Vec3 start, Vec3 end, boolean red) {
        if (Minecraft.getInstance().level == null) return;

        // Фиксируем сид, чтобы зигзаг не дергался во время рендеринга кадров
        long seed = Minecraft.getInstance().level.random.nextLong();

        // Время жизни линии на экране — 5 тиков (0.25 сек)
        ACTIVE_SEGMENTS.add(new ClientSegment(start, end, seed, 5, red));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;

        Iterator<ClientSegment> iterator = ACTIVE_SEGMENTS.iterator();
        while (iterator.hasNext()) {
            ClientSegment segment = iterator.next();
            segment.ticksLeft--;
            if (segment.ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ACTIVE_SEGMENTS.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (ClientSegment segment : ACTIVE_SEGMENTS) {
            drawLightningZigzags(buffer, poseStack, segment.start, segment.end, segment.seed, segment.red);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void drawLightningZigzags(VertexConsumer buffer, PoseStack matrix,
                                             Vec3 start, Vec3 end, long seed, boolean red) {
        RandomSource random = RandomSource.create(seed);
        Vec3 current = start;
        int segments = 8;

        // Толщина: 1 для синей, 4 для красной (имитируем параллельными копиями)
        int thickness = red ? 4 : 1;
        double zigzagOffset = red ? 0.7 : 0.4;

        // Цвета ядра
        float coreR = red ? 1.0F : 0.3F;
        float coreG = red ? 0.15F : 0.7F;
        float coreB = red ? 0.1F : 1.0F;

        // Цвета свечения
        float glowR = 1.0F;
        float glowG = red ? 0.6F : 1.0F;
        float glowB = red ? 0.4F : 1.0F;

        // Смещения для имитации толщины (микро-крест)
        float[][] offsets = {
                {0.0F, 0.0F, 0.0F},
                {0.02F, 0.0F, 0.0F},
                {-0.02F, 0.0F, 0.0F},
                {0.0F, 0.02F, 0.0F},
                {0.0F, -0.02F, 0.0F}
        };

        for (int i = 1; i <= segments; i++) {
            float progress = (float) i / segments;
            Vec3 target = start.lerp(end, progress);

            if (i < segments) {
                target = target.add(
                        (random.nextFloat() - 0.5) * zigzagOffset,
                        (random.nextFloat() - 0.5) * zigzagOffset,
                        (random.nextFloat() - 0.5) * zigzagOffset
                );
            }

            var pose = matrix.last().pose();

            for (int t = 0; t < thickness; t++) {
                float ox = offsets[t][0];
                float oy = offsets[t][1];
                float oz = offsets[t][2];

                // Линия 1 (ядро)
                buffer.addVertex(pose, (float) current.x + ox, (float) current.y + oy, (float) current.z + oz)
                        .setColor(coreR, coreG, coreB, 1.0F)
                        .setNormal(0.0F, 1.0F, 0.0F);

                buffer.addVertex(pose, (float) target.x + ox, (float) target.y + oy, (float) target.z + oz)
                        .setColor(coreR, coreG, coreB, 1.0F)
                        .setNormal(0.0F, 1.0F, 0.0F);

                // Линия 2 (внутреннее свечение)
                buffer.addVertex(pose, (float) current.x + ox, (float) current.y + 0.01F + oy, (float) current.z + oz)
                        .setColor(glowR, glowG, glowB, 1.0F)
                        .setNormal(0.0F, 1.0F, 0.0F);

                buffer.addVertex(pose, (float) target.x + ox, (float) target.y + 0.01F + oy, (float) target.z + oz)
                        .setColor(glowR, glowG, glowB, 1.0F)
                        .setNormal(0.0F, 1.0F, 0.0F);
            }

            current = target;
        }
    }

    private static class ClientSegment {
        final Vec3 start;
        final Vec3 end;
        final long seed;
        final boolean red;
        int ticksLeft;

        ClientSegment(Vec3 start, Vec3 end, long seed, int ticksLeft, boolean red) {
            this.start = start;
            this.end = end;
            this.seed = seed;
            this.ticksLeft = ticksLeft;
            this.red = red;
        }
    }
}