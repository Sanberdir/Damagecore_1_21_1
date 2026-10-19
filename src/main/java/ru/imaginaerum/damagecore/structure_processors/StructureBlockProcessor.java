package ru.imaginaerum.damagecore.structure_processors;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructureBlockProcessor extends StructureProcessor {
    public static final MapCodec<StructureBlockProcessor> CODEC =
            MapCodec.unit(StructureBlockProcessor::new);

    private static final Set<Block> FORBIDDEN = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MYCELIUM,
            Blocks.DIRT_PATH, Blocks.FARMLAND,Blocks.COAL_ORE,
            Blocks.DIORITE, Blocks.ANDESITE, Blocks.GRANITE
    );

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset,
                                                             BlockPos pos, StructureTemplate.StructureBlockInfo raw,
                                                             StructureTemplate.StructureBlockInfo relative, StructurePlaceSettings settings) {
        // Всё, что не плейсхолдер, не трогаем
        if (!relative.state().is(BlocksDC.STRUCTURE_BLOCK_AND_ENTITY.get())) return relative;

        CompoundTag nbt = relative.nbt();
        if (nbt == null) return relative;

        List<String> ents = read(nbt, "entity_ids");

        // Оставляем только валидные и разрешённые блоки
        List<Block> blocks = new ArrayList<>();
        for (String id : read(nbt, "block_ids")) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) continue;
            Block b = BuiltInRegistries.BLOCK.get(rl);
            if (FORBIDDEN.contains(b)) continue;
            blocks.add(b);
        }

        int total = ents.size() + blocks.size();
        if (total == 0) {
            // Безопасная замена, чтобы плейсхолдер не остался в мире
            return new StructureTemplate.StructureBlockInfo(
                    relative.pos(), Blocks.STONE_BRICKS.defaultBlockState(), null);
        }

        RandomSource random = settings.getRandom(relative.pos());
        int i = random.nextInt(total);

        if (i < ents.size()) {
            CompoundTag copy = nbt.copy();
            copy.putString("spawn_entity", ents.get(i));
            copy.putBoolean("from_structure", true);
            return new StructureTemplate.StructureBlockInfo(relative.pos(), relative.state(), copy);
        }
        return new StructureTemplate.StructureBlockInfo(
                relative.pos(), blocks.get(i - ents.size()).defaultBlockState(), null);
    }

    private static List<String> read(CompoundTag tag, String key) {
        ListTag l = tag.getList(key, Tag.TAG_STRING);
        List<String> r = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) r.add(l.getString(i));
        return r;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModProcessors.STRUCTURE_BLOCK.get();
    }
}