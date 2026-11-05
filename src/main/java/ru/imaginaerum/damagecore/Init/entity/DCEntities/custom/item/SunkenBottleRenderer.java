package ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item;

import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.items.custom.ChestOfWildCurrentsBlockItem;
import ru.imaginaerum.damagecore.Init.items.custom.SunkenBottleBlockItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SunkenBottleRenderer extends GeoItemRenderer<SunkenBottleBlockItem> {
    public SunkenBottleRenderer() {
        super(new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sunken_bottle")));
    }
}