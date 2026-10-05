package ru.imaginaerum.damagecore.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import ru.imaginaerum.damagecore.Init.items.DCItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TomeLocator {
    public static final int COMPASS_STEP = 10;

    private static final ResourceKey<Structure> CASTLE = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("damagecore", "pillager_castle"));

    private record Cache(BlockPos origin, BlockPos target) {}
    private static final Map<UUID, Cache> CACHE = new HashMap<>();

    private TomeLocator() {}
    public static boolean holdsTome(ServerPlayer p) {
        Item tome = DCItems.TOME_OF_ATTAINING_MEANINGS.get();
        return p.getMainHandItem().is(tome) || p.getOffhandItem().is(tome);
    }

    public static void toggle(ServerPlayer p) {
        if (p.getData(ModAttachments.TOME_STEP) != COMPASS_STEP) return;
        if (!holdsTome(p)) return;

        boolean on = !p.getData(ModAttachments.TOME_COMPASS_ON);
        p.setData(ModAttachments.TOME_COMPASS_ON, on);

        if (on) {
            update(p);
        } else {
            p.setData(ModAttachments.TOME_TARGET, Long.MIN_VALUE);
        }
    }

    /** Вызывать каждый тик игрока. */
    public static void tick(ServerPlayer p) {
        if (!p.getData(ModAttachments.TOME_COMPASS_ON)) return;

        if (p.getData(ModAttachments.TOME_STEP) != COMPASS_STEP || !holdsTome(p)) {
            p.setData(ModAttachments.TOME_COMPASS_ON, false);
            p.setData(ModAttachments.TOME_TARGET, Long.MIN_VALUE);
            return;
        }

        if (p.tickCount % 20 == 0) update(p);
    }
    public static void update(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos from = player.blockPosition();

        // 1) известный замок, где жив пленный житель
        CaptiveData data = CaptiveData.get(level);
        data.validate(level);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : data.snapshot().values()) {
            double d = p.distSqr(from);
            if (d < bestD) { bestD = d; best = p; }
        }

        // 2) иначе - ближайший ещё не сгенерированный замок
        if (best == null) {
            Cache c = CACHE.get(player.getUUID());
            if (c != null && c.origin().distSqr(from) < 96 * 96
                    && !level.getChunkSource().hasChunk(c.target().getX() >> 4, c.target().getZ() >> 4)) {
                best = c.target();
            } else {
                best = findUnexplored(level, from);
                if (best != null) CACHE.put(player.getUUID(), new Cache(from, best));
                else CACHE.remove(player.getUUID());
            }
        }

        player.setData(ModAttachments.TOME_TARGET, best == null ? Long.MIN_VALUE : best.asLong());
    }

    private static BlockPos findUnexplored(ServerLevel level, BlockPos from) {
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(CASTLE);
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> r = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 64, true);
        return r == null ? null : r.getFirst();
    }
}