package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               @Nullable ExistingFileHelper helper) {
        super(output, lookup, Damagecore_1_21_1_neo.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Ломается топором
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(BlocksDC.SUNKEN_STAIRS.get())
                .add(BlocksDC.SUNKEN_SLAB.get())
                .add(BlocksDC.SUNKEN_FENCE.get())
                .add(BlocksDC.SUNKEN_BARREL.get())
                .add(BlocksDC.SUNKEN_FENCE_GATE.get())
                .add(BlocksDC.SUNKEN_BUTTON.get())
                .add(BlocksDC.SUNKEN_PRESSURE_PLATE.get());

        // Деревянные группы (они уже входят в stairs/slabs/fences/buttons/pressure_plates)
        tag(BlockTags.WOODEN_STAIRS).add(BlocksDC.SUNKEN_STAIRS.get());
        tag(BlockTags.PLANKS).add(BlocksDC.SUNKEN_PLANKS.get());
        tag(BlockTags.WOODEN_FENCES).add(BlocksDC.SUNKEN_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(BlocksDC.SUNKEN_FENCE_GATE.get());
        tag(BlockTags.WOODEN_BUTTONS).add(BlocksDC.SUNKEN_BUTTON.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(BlocksDC.SUNKEN_PRESSURE_PLATE.get());

        // Теги NeoForge (для совместимости с другими модами)
        tag(Tags.Blocks.FENCE_GATES_WOODEN).add(BlocksDC.SUNKEN_FENCE_GATE.get());

        // Если досок ещё нет в тегах:
        // tag(BlockTags.PLANKS).add(BlocksDC.SUNKEN_PLANKS.get());
    }
}