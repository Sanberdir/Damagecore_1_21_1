package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class CastleGuardData extends SavedData {
    private static final String NAME = "damagecore_castle_guards";
    private final Set<BlockPos> points = new HashSet<>();

    public static CastleGuardData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CastleGuardData::new, CastleGuardData::load, null), NAME);
    }

    public static CastleGuardData load(CompoundTag tag, HolderLookup.Provider provider) {
        CastleGuardData data = new CastleGuardData();
        for (long l : tag.getLongArray("points")) {
            data.points.add(BlockPos.of(l));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        synchronized (points) {
            tag.putLongArray("points", points.stream().mapToLong(BlockPos::asLong).toArray());
        }
        return tag;
    }

    // Set убирает дубли: одна и та же точка не добавится дважды
    public void addPoint(BlockPos pos) {
        synchronized (points) {
            if (points.add(pos.immutable())) {
                setDirty();
            }
        }
    }

    public Set<BlockPos> getPoints() {
        synchronized (points) {
            return new HashSet<>(points);
        }
    }
}