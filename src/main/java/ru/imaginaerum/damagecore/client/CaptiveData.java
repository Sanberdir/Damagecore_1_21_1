package ru.imaginaerum.damagecore.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import ru.imaginaerum.damagecore.structure_processors.RescuableVillager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaptiveData extends SavedData {
    private static final String NAME = "damagecore_captives";
    private final Map<UUID, BlockPos> map = new HashMap<>();

    public static CaptiveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CaptiveData::new, CaptiveData::load, null), NAME);
    }

    public static CaptiveData load(CompoundTag tag, HolderLookup.Provider provider) {
        CaptiveData d = new CaptiveData();
        for (Tag t : tag.getList("captives", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.map.put(c.getUUID("id"), BlockPos.of(c.getLong("pos")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        synchronized (map) {
            map.forEach((id, pos) -> {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", id);
                c.putLong("pos", pos.asLong());
                list.add(c);
            });
        }
        tag.put("captives", list);
        return tag;
    }

    public void add(UUID id, BlockPos pos) {
        synchronized (map) { map.put(id, pos.immutable()); }
        setDirty();
    }

    public void remove(UUID id) {
        synchronized (map) { if (map.remove(id) != null) setDirty(); }
    }

    public Map<UUID, BlockPos> snapshot() {
        synchronized (map) { return new HashMap<>(map); }
    }

    /** Убирает записи о жителях, которых больше нет (убит, превращён, исчез). */
    public void validate(ServerLevel level) {
        for (Map.Entry<UUID, BlockPos> e : snapshot().entrySet()) {
            Entity ent = level.getEntity(e.getKey());
            if (ent != null) {
                if (!ent.isAlive() || !ent.getTags().contains(RescuableVillager.TAG)) remove(e.getKey());
            } else if (level.isPositionEntityTicking(e.getValue())) {
                remove(e.getKey()); // область загружена, а жителя нет
            }
        }
    }
}