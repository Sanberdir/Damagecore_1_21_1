package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, "damagecore");

    public static final Supplier<StructureProcessorType<StructureBlockProcessor>> STRUCTURE_BLOCK =
            PROCESSORS.register("structure_block", () -> () -> StructureBlockProcessor.CODEC);
}