package ru.imaginaerum.damagecore.entity.chest_of_wild_currents;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

public class SunkenBarrelModel extends DefaultedEntityGeoModel<SunkenBarrelEntity> {
    private static final ResourceLocation OPEN_MODEL =
            ResourceLocation.fromNamespaceAndPath(MODID, "geo/entity/sunken_barrel_open.geo.json");

    public SunkenBarrelModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "sunken_barrel"));
    }

    @Override
    public ResourceLocation getModelResource(SunkenBarrelEntity entity) {
        return entity.isOpen() ? OPEN_MODEL : super.getModelResource(entity);
    }

    // Текстура общая для обеих моделей. Если у открытой своя, верните её здесь
    // по тому же принципу через getTextureResource.
}