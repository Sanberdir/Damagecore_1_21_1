package ru.imaginaerum.damagecore.client.aspects;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ru.imaginaerum.damagecore.client.ModAttachments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class ElementOverlay {

    private static final int TEX_SIZE = 64;

    /** Длительность анимации в ТИКАХ. */
    private static final int ANIM_TICKS = 33;

    private static final ResourceLocation TERRA = tex("terra");
    private static final ResourceLocation AQUA  = tex("aqua");
    private static final ResourceLocation IGNIS = tex("ignis");
    private static final ResourceLocation AER   = tex("aer");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("damagecore", "textures/aspects/" + name + ".png");
    }

    // ---- параметры атласа частиц (как в Thaumcraft) ----
    private static final int PARTICLE_GRID      = 64;   // 64x64 кадра в атласе
    private static final int BURST_FRAME_START  = 320;  // первый кадр "вспышки"
    private static final int BURST_FRAME_SPREAD = 16;   // длина блока вспышек
    private static final int SPARK_FRAME        = 512;  // "обычная" искра

    private static LocalPlayer lastPlayer = null;
    private static int lastMask = 0;

    // ---- активная анимация значка ----
    private static ResourceLocation current = null;
    private static float[] color = {1F, 1F, 1F};
    private static int progress = 0;
    private static int maxProgress = ANIM_TICKS;
    private static long seed = 0L;

    // ---- частицы ----
    private static final int MAX_SPARKS = 200;
    private static final float SPARK_SCALE = 24.0F;
    private static final float[] SPARK_ALPHA_KEYS = {0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};
    private static final List<GuiSpark> SPARKS = new ArrayList<>();

    // =====================================================================
    //  ТИК
    // =====================================================================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            lastPlayer = null;
            SPARKS.clear();
            current = null;
            return;
        }

        int mask = player.getData(ModAttachments.TOME_ELEMENTS);

        if (player != lastPlayer) {
            lastPlayer = player;
            lastMask = mask;
            return;
        }

        int added = mask & ~lastMask;
        lastMask = mask;

        if (added != 0) {
            if ((added & 1) != 0) start(TERRA, 0.10F, 0.45F, 0.15F);
            if ((added & 2) != 0) start(AQUA,  0.35F, 0.70F, 1.00F);
            if ((added & 4) != 0) start(IGNIS, 1.00F, 0.50F, 0.10F);
            if ((added & 8) != 0) start(AER,   1.00F, 0.85F, 0.25F);
        }

        if (current != null) {
            progress--;
            if (progress <= 0) current = null;
        }

        tickSparks();
    }

    private static void start(ResourceLocation texture, float r, float g, float b) {
        current = texture;
        color = new float[]{r, g, b};
        progress = ANIM_TICKS;
        maxProgress = ANIM_TICKS;
        seed = System.nanoTime();
    }

    // =====================================================================
    //  РЕНДЕР
    // =====================================================================

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        int ww = graphics.guiWidth();
        int hh = graphics.guiHeight();

        // частицы СНАЧАЛА — иконка ляжет поверх
        renderSparks(graphics, partial);

        if (current != null) {
            renderIcon(graphics, ww, hh, partial);
        }
    }

    private static void renderIcon(GuiGraphics graphics, int ww, int hh, float partial) {
        Random rand = new Random(seed);

        float t = 1.0F - (progress - partial) / (float) maxProgress;
        t = Mth.clamp(t, 0.0F, 1.0F);

        float baseX = ww / 4.0F + rand.nextInt(32);
        float baseY = hh / 3.0F + rand.nextInt(32);

        float s = 16.0F;
        float x, y;

        if (t > 0.34F) {
            // ФАЗА 2: летит к книге + уменьшается
            float q = (1.0F - t) / 0.66F;
            s *= q;
            float m = (float) Math.sin(q * Math.PI - (Math.PI / 2)) * 0.5F + 0.5F;
            float d = (float) Math.sin(m * Math.PI * 0.5);
            x = baseX * d;
            y = baseY * m;
        } else {
            // ФАЗА 1: появляется + пульсирует размером
            float q = t / 0.34F;
            float m = (float) Math.sin(q * Math.PI * 2.0 - (Math.PI / 2)) * 0.5F + 1.5F;
            if (q < 0.5F) s *= q * 2.0F;
            s *= m;
            x = baseX;
            y = baseY;
        }

        float alpha;
        if (t < 0.1F) alpha = t / 0.1F;
        else if (t > 0.9F) alpha = (1.0F - t) / 0.1F;
        else alpha = 1.0F;
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        float xx = ww - 12 + rand.nextInt(8) - x;
        float yy = hh - 12 + rand.nextInt(8) - y;

        // ---- иконка ----
        graphics.pose().pushPose();
        graphics.pose().translate(xx, yy, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(84 + rand.nextInt(12) - 90));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(color[0], color[1], color[2], alpha);

        graphics.pose().pushPose();
        graphics.pose().translate(-s / 2.0F, -s / 2.0F, 0.0F);
        graphics.pose().scale(s / TEX_SIZE, s / TEX_SIZE, 1.0F);
        graphics.blit(current, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        graphics.pose().popPose();

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        // вспышки
        if (t > 0.9F) drawBurst(graphics, rand, (1.0F - t) / 0.1F, 64.0F);
        if (t < 0.1F) drawBurst(graphics, rand, t / 0.1F, 32.0F);

        graphics.pose().popPose();

        // ---- спавн частиц ----
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getRandom()
                .nextInt((int) (1.0F + t * 10.0F)) == 0) {
            spawnSpark(xx, yy);
        }
    }

    // =====================================================================
    //  ЧАСТИЦЫ — спрайты из атласа, окрашенные через setShaderColor
    // =====================================================================

    private static void spawnSpark(float x, float y) {
        if (SPARKS.size() >= MAX_SPARKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RandomSource rand = mc.level.getRandom();
        SPARKS.add(new GuiSpark(
                x + (float) rand.nextGaussian() * 5.0F,
                y + (float) rand.nextGaussian() * 5.0F,
                (float) rand.nextGaussian() * 2.0F,
                (float) rand.nextGaussian() * 2.0F,
                32 + rand.nextInt(8),
                rand.nextInt(5),
                rand.nextFloat() < 0.2F ? BURST_FRAME_START : SPARK_FRAME,
                Mth.nextInt(rand, 189, 255) / 255.0F,   // g
                Mth.nextInt(rand, 64,  255) / 255.0F)); // b
    }

    private static void renderSparks(GuiGraphics graphics, float partial) {
        if (SPARKS.isEmpty()) return;
        float texFrame = 1024.0F / PARTICLE_GRID;

        for (GuiSpark spark : SPARKS) {
            if (spark.delay > 0) continue;

            float life = (spark.age + partial) / spark.maxAge;
            float alpha = sampleKeys(SPARK_ALPHA_KEYS, life);
            if (alpha <= 0.0F) continue;

            float size = 0.2F * SPARK_SCALE * (1.0F + life);

            int frame  = spark.startFrame + spark.age % BURST_FRAME_SPREAD;
            int frameU = frame % PARTICLE_GRID;
            int frameV = frame / PARTICLE_GRID;

            float x = spark.xo + (spark.x - spark.xo) * partial;
            float y = spark.yo + (spark.y - spark.yo) * partial;

            graphics.pose().pushPose();
            graphics.pose().translate(x - size / 2.0F, y - size / 2.0F, 0.0F);
            graphics.pose().scale(size / texFrame, size / texFrame, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE);          // аддитивное свечение
            RenderSystem.setShaderColor(1.0F, spark.g, spark.b, alpha);  // ← ЦВЕТ тут
            graphics.blit(ParticleTextures.PARTICLES,
                    0, 0,
                    frameU * texFrame, frameV * texFrame,
                    (int) texFrame, (int) texFrame,
                    1024, 1024);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);         // сброс
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            graphics.pose().popPose();
        }
    }

    private static float sampleKeys(float[] keys, float life) {
        float position = Mth.clamp(life, 0.0F, 1.0F) * (keys.length - 1);
        int index = Math.min((int) position, keys.length - 2);
        float t = position - index;
        return keys[index] + (keys[index + 1] - keys[index]) * t;
    }

    private static void tickSparks() {
        Minecraft mc = Minecraft.getInstance();
        if (SPARKS.isEmpty()) return;

        Iterator<GuiSpark> it = SPARKS.iterator();
        while (it.hasNext()) {
            GuiSpark spark = it.next();
            if (spark.delay > 0) {
                spark.delay--;
                continue;
            }
            spark.age++;
            spark.xo = spark.x;
            spark.yo = spark.y;
            spark.x += spark.vx;
            spark.y += spark.vy;
            spark.vx *= 0.9F;
            spark.vy *= 0.9F;
            spark.vy += 0.04F;
            if (mc.level != null) {
                spark.vx += (float) mc.level.getRandom().nextGaussian() * 0.025F;
                spark.vy += (float) mc.level.getRandom().nextGaussian() * 0.025F;
            }
            if (spark.age >= spark.maxAge) it.remove();
        }
    }

    // =====================================================================
    //  ВСПЫШКА — тоже спрайт, тоже окрашена
    // =====================================================================

    private static void drawBurst(GuiGraphics graphics, Random rand, float phase, float baseSize) {
        float m = (float) Math.sin(phase * Math.PI * 2.0 - (Math.PI / 2)) * 0.25F + 0.25F;
        float size = baseSize * m;
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-rand.nextInt(360)));

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float g = Mth.nextInt(mc.level.getRandom(), 189, 255) / 255.0F;
        float b = Mth.nextInt(mc.level.getRandom(),  64, 255) / 255.0F;

        int frame  = BURST_FRAME_START + rand.nextInt(BURST_FRAME_SPREAD);
        int frameU = frame % PARTICLE_GRID;
        int frameV = frame / PARTICLE_GRID;
        float texFrame = 1024.0F / PARTICLE_GRID;

        graphics.pose().pushPose();
        graphics.pose().translate(-size / 2.0F, -size / 2.0F, 0.0F);
        graphics.pose().scale(size / texFrame, size / texFrame, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, g, b, 0.78F);   // ← цвет вспышки
        graphics.blit(ParticleTextures.PARTICLES,
                0, 0,
                frameU * texFrame, frameV * texFrame,
                (int) texFrame, (int) texFrame,
                1024, 1024);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        graphics.pose().popPose();
    }

    // =====================================================================
    //  ВНУТРЕННИЕ КЛАССЫ
    // =====================================================================

    private static final class GuiSpark {
        float x, y;
        float xo, yo;
        float vx, vy;
        int age;
        final int maxAge;
        int delay;
        final int startFrame;
        final float g;
        final float b;

        GuiSpark(float x, float y, float vx, float vy,
                 int maxAge, int delay,
                 int startFrame, float g, float b) {
            this.x = x;
            this.y = y;
            this.xo = x;
            this.yo = y;
            this.vx = vx;
            this.vy = vy;
            this.maxAge = maxAge;
            this.delay = delay;
            this.startFrame = startFrame;
            this.g = g;
            this.b = b;
        }
    }
}