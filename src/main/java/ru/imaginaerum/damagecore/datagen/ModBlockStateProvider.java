package ru.imaginaerum.damagecore.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Damagecore_1_21_1_neo.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Если у досок ещё нет модели/блокстейта, раскомментируй:
        // simpleBlockWithItem(BlocksDC.SUNKEN_PLANKS.get(),
        //         cubeAll(BlocksDC.SUNKEN_PLANKS.get()));

        ResourceLocation planks = blockTexture(BlocksDC.SUNKEN_PLANKS.get());

        stairsBlock((StairBlock) BlocksDC.SUNKEN_STAIRS.get(), planks);

        // третий аргумент: модель полного блока для двойной плиты
        slabBlock((SlabBlock) BlocksDC.SUNKEN_SLAB.get(), planks, planks);
        // ^ здесь (block, doubleSlabModel, texture), см. ниже

        fenceBlock((FenceBlock) BlocksDC.SUNKEN_FENCE.get(), planks);
        fenceGateBlock((FenceGateBlock) BlocksDC.SUNKEN_FENCE_GATE.get(), planks);
        buttonBlock((ButtonBlock) BlocksDC.SUNKEN_BUTTON.get(), planks);
        pressurePlateBlock((PressurePlateBlock) BlocksDC.SUNKEN_PRESSURE_PLATE.get(), planks);
    }
}