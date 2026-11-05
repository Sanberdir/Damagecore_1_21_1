package ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.items_box;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Свечение без записи в буфер глубины: не вырезает воду и не перекрывает предметы. */
public final class DCRenderTypes extends RenderType {
    private DCRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufSize,
                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    public static final RenderType GLOW = RenderType.create(
            "damagecore_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
}