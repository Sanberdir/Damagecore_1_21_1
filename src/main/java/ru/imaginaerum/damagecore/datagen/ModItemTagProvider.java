package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                              @Nullable ExistingFileHelper helper) {
        super(output, lookup, blockTags, Damagecore_1_21_1_neo.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.WOODEN_STAIRS).add(BlocksDC.SUNKEN_STAIRS.get().asItem());
        tag(ItemTags.PLANKS).add(BlocksDC.SUNKEN_PLANKS.get().asItem());
        tag(ItemTags.WOODEN_FENCES).add(BlocksDC.SUNKEN_FENCE.get().asItem());
        tag(ItemTags.FENCE_GATES).add(BlocksDC.SUNKEN_FENCE_GATE.get().asItem());
        tag(ItemTags.WOODEN_BUTTONS).add(BlocksDC.SUNKEN_BUTTON.get().asItem());
        tag(ItemTags.WOODEN_PRESSURE_PLATES).add(BlocksDC.SUNKEN_PRESSURE_PLATE.get().asItem());

        tag(Tags.Items.FENCE_GATES_WOODEN).add(BlocksDC.SUNKEN_FENCE_GATE.get().asItem());

        // tag(ItemTags.PLANKS).add(BlocksDC.SUNKEN_PLANKS.get().asItem());
    }
}