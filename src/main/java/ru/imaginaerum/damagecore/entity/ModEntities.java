package ru.imaginaerum.damagecore.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.entity.sunken_lockbox.SunkenLockboxEntity;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SunkenLockboxEntity>> SUNKEN_LOCKBOX =
            ENTITY_TYPES.register("sunken_lockbox", () ->
                    EntityType.Builder.<SunkenLockboxEntity>of(SunkenLockboxEntity::new, MobCategory.MISC)
                            .sized(1.0f, 0.9f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("sunken_lockbox"));
}