package ru.imaginaerum.damagecore.Init.fishing.weight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.Mth;

public final class BendConsumer implements VertexConsumer {
    private final VertexConsumer d;
    private final float bend;

    public BendConsumer(VertexConsumer d, float bend) { this.d = d; this.bend = bend; }

    private interface Emit { void accept(BakedQuad q); }

    /** Режем квад на сетку и гнём каждую вершину одной функцией. */
    private void tessellate(BakedQuad quad, Emit out) {
        int[] src = quad.getVertices();

        float[][] p = new float[4][5];           // x y z u v
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            p[i][0] = Float.intBitsToFloat(src[o]);
            p[i][1] = Float.intBitsToFloat(src[o + 1]);
            p[i][2] = Float.intBitsToFloat(src[o + 2]);
            p[i][3] = Float.intBitsToFloat(src[o + 4]);
            p[i][4] = Float.intBitsToFloat(src[o + 5]);
        }

        // число делений по длине рёбер (≈ 1 деление на пиксель, максимум 16)
        int nu = divisions(p[0], p[1]);
        int nv = divisions(p[0], p[3]);

        for (int iu = 0; iu < nu; iu++) {
            for (int iv = 0; iv < nv; iv++) {
                float s0 = iu / (float) nu, s1 = (iu + 1) / (float) nu;
                float t0 = iv / (float) nv, t1 = (iv + 1) / (float) nv;

                int[] v = new int[32];
                // тот же порядок обхода, что у исходного квада: (0,0) (1,0) (1,1) (0,1)
                put(v, 0, src, p, s0, t0);
                put(v, 1, src, p, s1, t0);
                put(v, 2, src, p, s1, t1);
                put(v, 3, src, p, s0, t1);

                out.accept(new BakedQuad(v, quad.getTintIndex(), quad.getDirection(),
                        quad.getSprite(), quad.isShade()));
            }
        }
    }
    public static float offsetAt(float bend, float x, float y) {
        float sp = Mth.clamp((x + y) * 0.5f, 0f, 1f);
        float w = Math.max(0f, (sp - 0.15f) / 0.85f);
        w = w * w * (0.6f + 0.4f * w);
        return bend * w * 0.7071f;
    }
    private static int divisions(float[] a, float[] b) {
        float dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        return Mth.clamp(Mth.ceil(len * 16f), 1, 16);
    }

    private void put(int[] dst, int idx, int[] src, float[][] p, float s, float t) {
        float x = bil(p, 0, s, t), y = bil(p, 1, s, t), z = bil(p, 2, s, t);
        float u = bil(p, 3, s, t), vv = bil(p, 4, s, t);

        // изгиб: кончик уходит вперёд и вниз
        float sp = Mth.clamp((x + y) * 0.5f, 0f, 1f);
        float w = Math.max(0f, (sp - 0.15f) / 0.85f);
        w = w * w * (0.6f + 0.4f * w);
        float off = bend * w * 0.7071f;
        x += off;
        y -= off;

        int o = idx * 8;
        dst[o]     = Float.floatToRawIntBits(x);
        dst[o + 1] = Float.floatToRawIntBits(y);
        dst[o + 2] = Float.floatToRawIntBits(z);
        dst[o + 3] = src[3];                       // цвет
        dst[o + 4] = Float.floatToRawIntBits(u);
        dst[o + 5] = Float.floatToRawIntBits(vv);
        dst[o + 6] = src[6];                       // lightmap
        dst[o + 7] = src[7];                       // нормаль
    }

    // билинейная интерполяция: рёбра 0-1 и 3-2 параллельны
    private static float bil(float[][] p, int c, float s, float t) {
        float a = p[0][c] + (p[1][c] - p[0][c]) * s;
        float b = p[3][c] + (p[2][c] - p[3][c]) * s;
        return a + (b - a) * t;
    }

    @Override
    public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a,
                            int light, int overlay) {
        tessellate(quad, q -> d.putBulkData(pose, q, r, g, b, a, light, overlay));
    }

    public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a,
                            int light, int overlay, boolean readAlpha) {
        tessellate(quad, q -> d.putBulkData(pose, q, r, g, b, a, light, overlay, readAlpha));
    }

    @Override
    public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness,
                            float r, float g, float b, float a, int[] lightmap, int overlay, boolean readAlpha) {
        tessellate(quad, q -> d.putBulkData(pose, q, brightness, r, g, b, a, lightmap, overlay, readAlpha));
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { return d.addVertex(x, y, z); }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return d.setColor(r, g, b, a); }
    @Override public VertexConsumer setUv(float u, float v) { return d.setUv(u, v); }
    @Override public VertexConsumer setUv1(int u, int v) { return d.setUv1(u, v); }
    @Override public VertexConsumer setUv2(int u, int v) { return d.setUv2(u, v); }
    @Override public VertexConsumer setNormal(float x, float y, float z) { return d.setNormal(x, y, z); }
}