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
     * Новый метод: принимает две точки напрямую от пакета стрелы
     */
    public static void addChainSegment(Vec3 start, Vec3 end) {
        if (Minecraft.getInstance().level == null) return;

        // Фиксируем сид, чтобы зигзаг не дергался во время рендеринга кадров
        long seed = Minecraft.getInstance().level.random.nextLong();

        // Время жизни линии на экране — 5 тиков (0.25 сек)
        ACTIVE_SEGMENTS.add(new ClientSegment(start, end, seed, 5));
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
            drawLightningZigzags(buffer, poseStack, segment.start, segment.end, segment.seed);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void drawLightningZigzags(VertexConsumer buffer, PoseStack matrix, Vec3 start, Vec3 end, long seed) {
        RandomSource random = RandomSource.create(seed);
        Vec3 current = start;
        int segments = 8; // Сделаем 8 изломов для большей детализации зигзага

        for (int i = 1; i <= segments; i++) {
            float progress = (float) i / segments;
            Vec3 target = start.lerp(end, progress);

            if (i < segments) {
                double offset = 0.4; // Амплитуда изломов ломаной линии
                target = target.add(
                        (random.nextFloat() - 0.5) * offset,
                        (random.nextFloat() - 0.5) * offset,
                        (random.nextFloat() - 0.5) * offset
                );
            }

            var pose = matrix.last().pose();

            // Линия 1 (Ядро молнии — неоново-голубая)
            buffer.addVertex(pose, (float)current.x, (float)current.y, (float)current.z)
                    .setColor(0.3F, 0.7F, 1.0F, 1.0F)
                    .setNormal(0.0F, 1.0F, 0.0F);

            buffer.addVertex(pose, (float)target.x, (float)target.y, (float)target.z)
                    .setColor(0.3F, 0.7F, 1.0F, 1.0F)
                    .setNormal(0.0F, 1.0F, 0.0F);

            // Линия 2 (Дополнительное внутреннее белое свечение со смещением для толщины)
            buffer.addVertex(pose, (float)current.x, (float)current.y + 0.01F, (float)current.z)
                    .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                    .setNormal(0.0F, 1.0F, 0.0F);

            buffer.addVertex(pose, (float)target.x, (float)target.y + 0.01F, (float)target.z)
                    .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                    .setNormal(0.0F, 1.0F, 0.0F);

            current = target;
        }
    }

    private static class ClientSegment {
        final Vec3 start;
        final Vec3 end;
        final long seed;
        int ticksLeft;

        ClientSegment(Vec3 start, Vec3 end, long seed, int ticksLeft) {
            this.start = start;
            this.end = end;
            this.seed = seed;
            this.ticksLeft = ticksLeft;
        }
    }
}
