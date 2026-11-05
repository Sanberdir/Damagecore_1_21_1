package ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.items_box.DCRenderTypes;

/** Маленькие лучи вокруг висящего предмета: веер во все стороны из одной точки. */
public final class HoverRays {
    private static final int RAYS = 9;
    private static final float START_R = 0.06f;   // от центра, чтобы корень не прятался в модели
    private static final int[][] PALETTE = {
            {110, 140, 255}, {140, 110, 255}, {170, 100, 255}, {200, 120, 255}, {90, 190, 255}
    };

    private HoverRays() {}
    /** Мягкое пятно света: три пересекающихся диска, яркие в центре и прозрачные по краю. */
    private static void halo(VertexConsumer c, Matrix4f m, float cy, float time) {
        float pulse = 0.8f + 0.2f * Mth.sin(time * 0.12f);
        int aCenter = (int) (95 * pulse);
        float r = 0.30f;
        int[] col = PALETTE[1];
        disc(c, m, col, cy, 1f, 0f, 0f, 0f, 0f, 1f, r, aCenter);   // горизонтальный (XZ)
        disc(c, m, col, cy, 1f, 0f, 0f, 0f, 1f, 0f, r, aCenter);   // вертикальный (XY)
        disc(c, m, col, cy, 0f, 0f, 1f, 0f, 1f, 0f, r, aCenter);   // вертикальный (ZY)
    }

    private static void disc(VertexConsumer c, Matrix4f m, int[] col, float cy,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float r, int aCenter) {
        final int SEG = 12;
        for (int k = 0; k < SEG; k++) {
            float a0 = k * Mth.TWO_PI / SEG, a1 = (k + 1) * Mth.TWO_PI / SEG;
            float c0 = Mth.cos(a0) * r, s0 = Mth.sin(a0) * r;
            float c1 = Mth.cos(a1) * r, s1 = Mth.sin(a1) * r;
            float[] p0 = {0f, cy, 0f, aCenter};
            float[] p1 = {ax * c0 + bx * s0, cy + ay * c0 + by * s0, az * c0 + bz * s0, 0};
            float[] p2 = {ax * c1 + bx * s1, cy + ay * c1 + by * s1, az * c1 + bz * s1, 0};
            vert(c, m, col, p0); vert(c, m, col, p1); vert(c, m, col, p2); vert(c, m, col, p2);
            vert(c, m, col, p2); vert(c, m, col, p2); vert(c, m, col, p1); vert(c, m, col, p0);   // обратная сторона
        }
    }
    /** ps: начало координат у ног предмета; cy: высота центра предмета. */
    public static void render(PoseStack ps, MultiBufferSource buf, float time, float cy) {
        VertexConsumer c = buf.getBuffer(DCRenderTypes.GLOW);
        Matrix4f m = ps.last().pose();
        halo(c, m, cy, time);

        for (int i = 0; i < RAYS; i++) {
            // направления равномерно по сфере (спираль), медленно вращаются
            float dy = 1f - 2f * (i + 0.5f) / RAYS;
            float rad = Mth.sqrt(Math.max(0f, 1f - dy * dy));
            float az = i * 2.399963f + time * 0.03f;
            float dx = rad * Mth.cos(az), dz = rad * Mth.sin(az);

            float flick = 0.6f + 0.4f * Mth.sin(time * 0.2f + i * 1.7f);
            float len = (0.28f + 0.22f * frac(i * 0.37f + 0.2f)) * flick;
            float w = 0.012f + 0.010f * frac(i * 0.53f);
            int a = (int) (170 * flick);
            int[] col = PALETTE[i % PALETTE.length];

            // две перпендикулярные плоскости, чтобы луч был виден с любой стороны
            float ux = 0f, uy = 1f, uz = 0f;
            if (Math.abs(dy) > 0.95f) { ux = 1f; uy = 0f; }
            float s1x = dy * uz - dz * uy, s1y = dz * ux - dx * uz, s1z = dx * uy - dy * ux;
            float sl = Mth.sqrt(s1x * s1x + s1y * s1y + s1z * s1z);
            s1x /= sl; s1y /= sl; s1z /= sl;
            float s2x = dy * s1z - dz * s1y, s2y = dz * s1x - dx * s1z, s2z = dx * s1y - dy * s1x;

            float ox = dx * START_R, oy = cy + dy * START_R, oz = dz * START_R;
            float tx = dx * (START_R + len), ty = cy + dy * (START_R + len), tz = dz * (START_R + len);

            ray(c, m, col, ox, oy, oz, tx, ty, tz, s1x, s1y, s1z, w, a);
            ray(c, m, col, ox, oy, oz, tx, ty, tz, s2x, s2y, s2z, w, a);

        }

    }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    /** Основание шириной 2w, сужается в точку, прозрачность падает к концу. */
    private static void ray(VertexConsumer c, Matrix4f m, int[] col,
                            float ox, float oy, float oz, float tx, float ty, float tz,
                            float sx, float sy, float sz, float w, int a) {
        float[][] p = {
                {ox - sx * w, oy - sy * w, oz - sz * w, a},
                {ox + sx * w, oy + sy * w, oz + sz * w, a},
                {tx, ty, tz, 0},
                {tx, ty, tz, 0}
        };
        for (float[] v : p) vert(c, m, col, v);                      // лицевая сторона
        for (int k = 3; k >= 0; k--) vert(c, m, col, p[k]);          // обратная
    }

    private static void vert(VertexConsumer c, Matrix4f m, int[] col, float[] v) {
        c.addVertex(m, v[0], v[1], v[2]).setColor(col[0], col[1], col[2], Mth.clamp((int) v[3], 0, 255));
    }
}