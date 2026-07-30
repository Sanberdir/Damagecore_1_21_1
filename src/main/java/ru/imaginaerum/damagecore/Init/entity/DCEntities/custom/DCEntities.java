package ru.imaginaerum.damagecore.Init.entity.DCEntities.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item.ChainLightningArrow;

public class DCEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "damagecore");

    // Регистрация нашей сущности стрелы
    public static final DeferredHolder<EntityType<?>, EntityType<ChainLightningArrow>> CHAIN_LIGHTNING_ARROW =
            ENTITIES.register("chain_lightning_arrow", () -> EntityType.Builder.<ChainLightningArrow>of(
                            ChainLightningArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // Размеры стрелы
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("chain_lightning_arrow"));

}