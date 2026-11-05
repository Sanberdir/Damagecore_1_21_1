package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox.items_box.DCRenderTypes;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Свет из открытой шкатулки: прямоугольное основание, столб по форме верха ящика и искры. */
public class SunkenLockboxGlowLayer extends GeoRenderLayer<SunkenLockboxEntity> {
    private static final float BEAM_H = 1.0f;     // высота столба
    private static final float INSET  = 0.04f;    // отступ от краёв ящика
    private static final float LIFT   = 0.003f;   // чтобы не мерцало на поверхности
    private static final int   SPARKS = 14;

    private static final int[] BLUE   = {110, 140, 255};
    private static final int[] PURPLE = {150, 100, 255};
    private static final int[] SPARK  = {205, 175, 255};

    private static final float[] ROW_Y = {0.00f, 0.45f, 1.00f};
    private static final float[] ROW_A = {1.00f, 0.50f, 0.00f};
    private static final float[] ROW_W = {1.00f, 1.06f, 1.14f};

    // центр и половинные размеры проёма, высота верха ящика (в блоках, пространство модели)
    private float cx = 0f, cz = 0f, hx = 0.33f, hz = 0.21f, topY = 0.5625f;
    private boolean measured;

    public SunkenLockboxGlowLayer(GeoRenderer<SunkenLockboxEntity> renderer) {
        super(renderer);
    }

    /** Берём верхний (самый высокий, затем самый широкий) куб кости crate. */
    private void measure(BakedGeoModel model) {
        measured = true;
        GeoBone bone = model.getBone("crate").orElse(null);
        if (bone == null) return;

        double bestTop = -1e9, bestArea = -1;
        double[] best = null;

        for (GeoCube cube : bone.getCubes()) {
            double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9, minZ = 1e9, maxZ = -1e9;
            for (GeoQuad q : cube.quads()) {
                if (q == null) continue;
                for (GeoVertex v : q.vertices()) {
                    var p = v.position();
                    minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
                    minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
                }
            }
            double area = (maxX - minX) * (maxZ - minZ);
            if (maxY > bestTop + 1e-4 || (Math.abs(maxY - bestTop) <= 1e-4 && area > bestArea)) {
                bestTop = maxY;
                bestArea = area;
                best = new double[]{minX, maxX, minZ, maxZ};
            }
        }
        if (best == null) return;

        cx = (float) ((best[0] + best[1]) / 2);
        cz = (float) ((best[2] + best[3]) / 2);
        hx = (float) ((best[1] - best[0]) / 2) - INSET;
        hz = (float) ((best[3] - best[2]) / 2) - INSET;
        topY = (float) bestTop + LIFT;
    }

    @Override
    public void render(PoseStack ps, SunkenLockboxEntity be, BakedGeoModel model,
                       @Nullable RenderType rt, MultiBufferSource buf,
                       @Nullable VertexConsumer vc, float pt, int light, int overlay) {
        float glow = be.getGlow();
        if (glow <= 0f) return;
        if (!measured) measure(model);

        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + pt;
        float g = glow * (0.88f + 0.12f * Mth.sin(time * 0.15f));

        VertexConsumer c = buf.getBuffer(DCRenderTypes.GLOW);

        ps.pushPose();
        ps.translate(cx, topY, cz);       // дальше всё считаем от центра верха ящика
        Matrix4f m = ps.last().pose();

        base(c, m, g);

        // Рисуем только внешние стенки столба света
        wall(c, m, -hx, -hz,  hx, -hz, BLUE,   g, time, 0.0f);
        wall(c, m,  hx, -hz,  hx,  hz, PURPLE, g, time, 1.4f);
        wall(c, m,  hx,  hz, -hx,  hz, BLUE,   g, time, 2.8f);
        wall(c, m, -hx,  hz, -hx, -hz, PURPLE, g, time, 4.2f);

        // Убран вызов core(...), разделявший луч на части

        sparks(c, m, g, time);
        ps.popPose();
    }

    private void base(VertexConsumer c, Matrix4f m, float g) {
        int ac = (int) (215 * g), ae = (int) (140 * g);
        float[][] k = {{-hx, -hz}, {hx, -hz}, {hx, hz}, {-hx, hz}};
        for (int i = 0; i < 4; i++) {
            float[] p = k[i], q = k[(i + 1) % 4];
            v(c, m, PURPLE, 0f, 0f, 0f, ac);
            v(c, m, PURPLE, p[0], 0f, p[1], ae);
            v(c, m, PURPLE, q[0], 0f, q[1], ae);
            v(c, m, PURPLE, q[0], 0f, q[1], ae);
        }
    }

    private void wall(VertexConsumer c, Matrix4f m, float x0, float z0, float x1, float z1,
                      int[] col, float g, float time, float phase) {
        float base = 120f * g * (0.85f + 0.15f * Mth.sin(time * 0.2f + phase));
        for (int r = 0; r < ROW_Y.length - 1; r++) {
            float ya = ROW_Y[r] * BEAM_H,  yb = ROW_Y[r + 1] * BEAM_H;
            float sa = ROW_W[r],           sb = ROW_W[r + 1];
            int aa = (int) (base * ROW_A[r]), ab = (int) (base * ROW_A[r + 1]);
            v(c, m, col, x0 * sa, ya, z0 * sa, aa);
            v(c, m, col, x1 * sa, ya, z1 * sa, aa);
            v(c, m, col, x1 * sb, yb, z1 * sb, ab);
            v(c, m, col, x0 * sb, yb, z0 * sb, ab);
        }
    }

    private void core(VertexConsumer c, Matrix4f m, float ax, float az, float halfW,
                      float g, float time, float phase) {
        float base = 75f * g * (0.85f + 0.15f * Mth.sin(time * 0.17f + phase));
        for (int r = 0; r < ROW_Y.length - 1; r++) {
            float ya = ROW_Y[r] * BEAM_H,  yb = ROW_Y[r + 1] * BEAM_H;
            int aa = (int) (base * ROW_A[r]), ab = (int) (base * ROW_A[r + 1]);
            v(c, m, BLUE, -ax * halfW, ya, -az * halfW, 0);
            v(c, m, BLUE, 0f, ya, 0f, aa);
            v(c, m, BLUE, 0f, yb, 0f, ab);
            v(c, m, BLUE, -ax * halfW, yb, -az * halfW, 0);
            v(c, m, BLUE, 0f, ya, 0f, aa);
            v(c, m, BLUE, ax * halfW, ya, az * halfW, 0);
            v(c, m, BLUE, ax * halfW, yb, az * halfW, 0);
            v(c, m, BLUE, 0f, yb, 0f, ab);
        }
    }

    private void sparks(VertexConsumer c, Matrix4f m, float g, float time) {
        for (int i = 0; i < SPARKS; i++) {
            float speed = 0.6f + 0.8f * frac(i * 0.37f);
            float ph = frac(time * 0.012f * speed + i * 0.173f);
            float x = (frac(i * 0.618f) * 2f - 1f) * hx * 0.8f + 0.04f * Mth.sin(time * 0.07f + i * 2.1f);
            float z = (frac(i * 0.382f + 0.3f) * 2f - 1f) * hz * 0.8f + 0.04f * Mth.sin(time * 0.06f + i * 1.3f);
            float y = 0.05f + ph * BEAM_H * 0.9f;
            float s = 0.014f + 0.012f * frac(i * 0.77f);
            int a = (int) (235 * g * Mth.sin(ph * Mth.PI));
            if (a <= 2) continue;

            v(c, m, SPARK, x - s, y - s, z, a); v(c, m, SPARK, x + s, y - s, z, a);
            v(c, m, SPARK, x + s, y + s, z, a); v(c, m, SPARK, x - s, y + s, z, a);
            v(c, m, SPARK, x, y - s, z - s, a); v(c, m, SPARK, x, y - s, z + s, a);
            v(c, m, SPARK, x, y + s, z + s, a); v(c, m, SPARK, x, y + s, z - s, a);
            v(c, m, SPARK, x - s, y, z - s, a); v(c, m, SPARK, x + s, y, z - s, a);
            v(c, m, SPARK, x + s, y, z + s, a); v(c, m, SPARK, x - s, y, z + s, a);
        }
    }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    private static void v(VertexConsumer c, Matrix4f m, int[] col, float x, float y, float z, int a) {
        c.addVertex(m, x, y, z).setColor(col[0], col[1], col[2], Mth.clamp(a, 0, 255));
    }
}