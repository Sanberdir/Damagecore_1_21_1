package ru.imaginaerum.damagecore.Init.blocks.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;
import ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.ChestOfWildCurrentsEntity;
import ru.imaginaerum.damagecore.Init.blocks.custom.sunken_bottle.SunkenBottleEntity;

import java.util.function.Supplier;

public class DCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Damagecore_1_21_1_neo.MODID);

    public static final Supplier<BlockEntityType<StructureBlockEntity>> STRUCTURE_BE =
            BLOCK_ENTITIES.register("structure_be", () ->
                    BlockEntityType.Builder.of(StructureBlockEntity::new,
                            BlocksDC.STRUCTURE_BLOCK_AND_ENTITY.get()).build(null));   // блок обязательно здесь

    public static final Supplier<BlockEntityType<ChestOfWildCurrentsEntity>> CHEST_OF_WILD_CURRENTS =
            BLOCK_ENTITIES.register("chest_of_wild_currents", () ->
                    BlockEntityType.Builder.of(ChestOfWildCurrentsEntity::new,
                            BlocksDC.CHEST_OF_WILD_CURRENTS.get()).build(null));
    public static final Supplier<BlockEntityType<SunkenBottleEntity>> SUNKEN_BOTTLE =
            BLOCK_ENTITIES.register("sunken_bottle", () ->
                    BlockEntityType.Builder.of(SunkenBottleEntity::new,
                            BlocksDC.SUNKEN_BOTTLE.get()).build(null));
}