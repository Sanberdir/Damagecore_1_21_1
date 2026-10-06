package ru.imaginaerum.damagecore.Init.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class StructureBlockEntity extends BlockEntity {
    private List<String> entityIds = new ArrayList<>();
    private List<String> blockIds = new ArrayList<>();
    private String spawnEntity = "";
    private boolean fromStructure = false;
    public StructureBlockEntity(BlockPos pos, BlockState state) {
        super(DCBlockEntities.STRUCTURE_BE.get(), pos, state);
    }

    public List<String> getEntityIds() { return List.copyOf(entityIds); }
    public List<String> getBlockIds() { return List.copyOf(blockIds); }

    public void setSelection(List<String> entityIds, List<String> blockIds) {
        this.entityIds = new ArrayList<>(entityIds);
        this.blockIds = new ArrayList<>(blockIds);
        setChanged();
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("entity_ids", toList(entityIds));
        tag.put("block_ids", toList(blockIds));
        System.out.println("SAVE BE " + worldPosition + " " + entityIds + " " + blockIds);
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        entityIds = fromList(tag.getList("entity_ids", Tag.TAG_STRING));
        blockIds = fromList(tag.getList("block_ids", Tag.TAG_STRING));
        spawnEntity = tag.getString("spawn_entity");
        fromStructure = tag.getBoolean("from_structure");
    }
    private static ListTag toList(List<String> src) {
        ListTag l = new ListTag();
        for (String s : src) l.add(StringTag.valueOf(s));
        return l;
    }

    private static List<String> fromList(ListTag l) {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) r.add(l.getString(i));
        return r;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StructureBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 10 != 0) return;
        if (!be.fromStructure) return;            // блок поставлен игроком - не трогаем
        if (be.spawnEntity.isEmpty()) return;

        ResourceLocation rl = ResourceLocation.tryParse(be.spawnEntity);
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
        sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        var e = type.spawn(sl, pos, MobSpawnType.STRUCTURE);
        if (e instanceof Mob m) m.setPersistenceRequired();
    }
}