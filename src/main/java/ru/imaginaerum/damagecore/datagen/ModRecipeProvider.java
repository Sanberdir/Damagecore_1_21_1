package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ItemLike planks = BlocksDC.SUNKEN_PLANKS.get();

        // Лестница: 6 досок -> 4 лестницы
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksDC.SUNKEN_STAIRS.get(), 4)
                .define('#', planks)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .group("wooden_stairs")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlocksDC.SUNKEN_BARREL.get())
                .define('P', BlocksDC.SUNKEN_PLANKS.get())
                .define('S', BlocksDC.SUNKEN_SLAB.get())
                .pattern("PSP")
                .pattern("P P")
                .pattern("PSP")
                .unlockedBy("has_sunken_planks", has(BlocksDC.SUNKEN_PLANKS.get()))
                .save(output);
        // Плита: 3 доски -> 6 плит
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlocksDC.SUNKEN_SLAB.get(), 6)
                .define('#', planks)
                .pattern("###")
                .group("wooden_slab")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);

        // Забор: 4 доски + 2 палки -> 3 забора
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, BlocksDC.SUNKEN_FENCE.get(), 3)
                .define('#', planks)
                .define('S', Items.STICK)
                .pattern("#S#")
                .pattern("#S#")
                .group("wooden_fence")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);

        // Калитка: 2 доски + 4 палки -> 1 калитка
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, BlocksDC.SUNKEN_FENCE_GATE.get())
                .define('#', Items.STICK)
                .define('W', planks)
                .pattern("#W#")
                .pattern("#W#")
                .group("wooden_fence_gate")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);

        // Кнопка: 1 доска -> 1 кнопка
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, BlocksDC.SUNKEN_BUTTON.get())
                .requires(planks)
                .group("wooden_button")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);

        // Нажимная плита: 2 доски -> 1 плита
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, BlocksDC.SUNKEN_PRESSURE_PLATE.get())
                .define('#', planks)
                .pattern("##")
                .group("wooden_pressure_plate")
                .unlockedBy("has_sunken_planks", has(planks))
                .save(output);
    }
}