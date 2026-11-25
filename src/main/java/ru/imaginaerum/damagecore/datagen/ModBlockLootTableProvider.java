package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.List;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    // Единый список, чтобы generate() и getKnownBlocks() не расходились
    private static List<Block> blocks() {
        return List.of(
                BlocksDC.SUNKEN_STAIRS.get(),
                BlocksDC.SUNKEN_SLAB.get(),
                BlocksDC.SUNKEN_FENCE.get(),
                BlocksDC.SUNKEN_FENCE_GATE.get(),
                BlocksDC.SUNKEN_BUTTON.get(),
                BlocksDC.SUNKEN_PRESSURE_PLATE.get()
        );
    }

    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // dropSelf(BlocksDC.SUNKEN_PLANKS.get()); // если у досок ещё нет лута

        dropSelf(BlocksDC.SUNKEN_STAIRS.get());
        // у плиты двойной блок дропает 2 штуки
        add(BlocksDC.SUNKEN_SLAB.get(), this::createSlabItemTable);
        dropSelf(BlocksDC.SUNKEN_FENCE.get());
        dropSelf(BlocksDC.SUNKEN_FENCE_GATE.get());
        dropSelf(BlocksDC.SUNKEN_BUTTON.get());
        dropSelf(BlocksDC.SUNKEN_PRESSURE_PLATE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return blocks();
    }
}