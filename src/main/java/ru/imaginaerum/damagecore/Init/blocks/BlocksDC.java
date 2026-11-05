package ru.imaginaerum.damagecore.Init.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
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

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        DCItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
