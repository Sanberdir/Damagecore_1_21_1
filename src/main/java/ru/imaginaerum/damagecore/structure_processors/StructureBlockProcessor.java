package ru.imaginaerum.damagecore.structure_processors;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;

import java.util.ArrayList;
import java.util.List;

public class StructureBlockProcessor extends StructureProcessor {
    public static final MapCodec<StructureBlockProcessor> CODEC =
            MapCodec.unit(StructureBlockProcessor::new);

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset,
                                                             BlockPos pos, StructureTemplate.StructureBlockInfo raw,
                                                             StructureTemplate.StructureBlockInfo relative, StructurePlaceSettings settings) {
        System.out.println("PROC " + relative.pos() + " " + relative.state() + " nbt=" + relative.nbt());
        if (!relative.state().is(BlocksDC.STRUCTURE_BLOCK_AND_ENTITY.get())) return relative;
        CompoundTag nbt = relative.nbt();
        if (nbt == null) return relative;

        List<String> ents = read(nbt, "entity_ids");
        List<String> blks = read(nbt, "block_ids");
        int total = ents.size() + blks.size();
        if (total == 0) return relative;

        RandomSource random = settings.getRandom(relative.pos());
        int i = random.nextInt(total);

        if (i < ents.size()) {
            CompoundTag copy = nbt.copy();
            copy.putString("spawn_entity", ents.get(i));
            copy.putBoolean("from_structure", true);
            return new StructureTemplate.StructureBlockInfo(relative.pos(), relative.state(), copy);
        }

        ResourceLocation rl = ResourceLocation.tryParse(blks.get(i - ents.size()));
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) return relative;
        Block block = BuiltInRegistries.BLOCK.get(rl);
        return new StructureTemplate.StructureBlockInfo(relative.pos(), block.defaultBlockState(), null);
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