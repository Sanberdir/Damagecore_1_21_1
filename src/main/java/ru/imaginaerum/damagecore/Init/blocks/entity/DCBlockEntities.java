package ru.imaginaerum.damagecore.Init.blocks.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.function.Supplier;

public class DCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Damagecore_1_21_1_neo.MODID);

    public static final Supplier<BlockEntityType<StructureBlockEntity>> STRUCTURE_BE =
            BLOCK_ENTITIES.register("structure_be", () ->
                    BlockEntityType.Builder.of(StructureBlockEntity::new,
                            BlocksDC.STRUCTURE_BLOCK_AND_ENTITY.get()).build(null));   // блок обязательно здесь
}