package ru.imaginaerum.damagecore.Init.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.custom.EmbodimentTableBlock;
import ru.imaginaerum.damagecore.Init.blocks.custom.SunkenBarrelBlock;
import ru.imaginaerum.damagecore.Init.blocks.custom.sunken_bottle.SunkenBottleBlock;
import ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.ChestOfWildCurrentsBlock;
import ru.imaginaerum.damagecore.Init.blocks.custom.StructureBlocksAndEntity;
import ru.imaginaerum.damagecore.Init.items.DCItems;

import java.util.function.Supplier;

public class BlocksDC {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Damagecore_1_21_1_neo.MODID);
    public static final DeferredBlock<Block> STRUCTURE_BLOCK_AND_ENTITY = registerBlock("structure_block_and_entity",
            () -> new StructureBlocksAndEntity(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> CHEST_OF_WILD_CURRENTS = BLOCKS.register("chest_of_wild_currents",
            () -> new ChestOfWildCurrentsBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    public static final DeferredBlock<Block> SUNKEN_BOTTLE = BLOCKS.register("sunken_bottle",
            () -> new SunkenBottleBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()));
    public static final DeferredBlock<Block> EMBODIMENT_TABLE = registerBlock("embodiment_table",
            () -> new EmbodimentTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .strength(5.0F, 1200.0F)));
    public static final DeferredBlock<Block> SUNKEN_BARREL = registerBlock("sunken_barrel",
            () -> new SunkenBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.NETHER_WOOD)
                    .noOcclusion()
                    .ignitedByLava()));
    public static final DeferredBlock<Block> SUNKEN_PLANKS = registerBlock("sunken_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.NETHER_WOOD)
                    .noOcclusion()
                    .ignitedByLava()));
    public static final DeferredBlock<Block> SUNKEN_STAIRS = registerBlock("sunken_stairs",
            () -> new StairBlock(SUNKEN_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SUNKEN_PLANKS.get())));
    public static final DeferredBlock<Block> SUNKEN_SLAB = registerBlock("sunken_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> SUNKEN_FENCE = registerBlock("sunken_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(SUNKEN_PLANKS.get().defaultMapColor())
                    .forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> SUNKEN_FENCE_GATE = registerBlock("sunken_fence_gate",
            () -> new FenceGateBlock(DCWoodType.SUNKEN_WOOD, BlockBehaviour.Properties.of().mapColor(SUNKEN_PLANKS.get()
                            .defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
                    .ignitedByLava()));
    public static final DeferredBlock<Block> SUNKEN_BUTTON = registerBlock("sunken_button",
            () -> new ButtonBlock(BlockSetType.OAK,10, BlockBehaviour.Properties.of().noCollission()
                    .strength(0.5F).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> SUNKEN_PRESSURE_PLATE = registerBlock("sunken_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(SUNKEN_PLANKS.get().defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission()
                    .strength(0.5F).ignitedByLava().pushReaction(PushReaction.DESTROY)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        DCItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
