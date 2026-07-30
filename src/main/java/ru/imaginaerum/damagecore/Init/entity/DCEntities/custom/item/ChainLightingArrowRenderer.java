package ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;


public class ChainLightingArrowRenderer extends ArrowRenderer<ChainLightningArrow> {

    public static final ResourceLocation FLAME_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    Damagecore_1_21_1_neo.MODID,
                    "textures/entity/projectiles/chain_light_arrow.png"
            );

    public ChainLightingArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ChainLightningArrow arrow) {
        return FLAME_ARROW;
    }
}