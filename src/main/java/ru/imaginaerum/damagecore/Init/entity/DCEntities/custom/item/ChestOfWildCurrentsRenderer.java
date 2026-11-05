package ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item;

import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.items.custom.ChestOfWildCurrentsBlockItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ChestOfWildCurrentsRenderer extends GeoItemRenderer<ChestOfWildCurrentsBlockItem> {
    public ChestOfWildCurrentsRenderer() {
        super(new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "chest_of_wild_currents")));
    }
}