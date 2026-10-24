package ru.imaginaerum.damagecore.Init.fishing.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.*;

/**
 * Заселение воды рыбой независимо от игрока.
 *  1) Загруженный чанк через 2 с сканируется: если в нём есть вода, он попадает в список водных
 *     и сразу заселяется рыбой (работает и в уже сгенерированных мирах).
 *  2) Из списка водных чанков периодически выбираются случайные и пополняются.
 *  Рыба помечается постоянной, чтобы не исчезала вдали от игрока; число ограничено CHUNK_CAP.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class FishSpawner {
    // сканирование и первичное заселение
    static final int   INIT_FILL     = 2;      // было 6
    static final int   REFILL_CHUNKS = 6;      // было 10
    static final float REFILL_CHANCE = 0.25f;  // было 0.9f
    static final int   REFILL_MAX    = 1;      // было 2
    static final int   CHUNK_CAP     = 3;      // было 12
    static final int SCAN_DELAY    = 40;   // тиков после загрузки (даём подгрузиться сущностям)
    static final int SCAN_PER_TICK = 6;    // чанков из очереди за тик

    // пополнение
    static final int   REFILL_PERIOD = 20;    // раз в секунду
    static final int TRIES     = 8;
    static final int MIN_DEPTH = 1;    // минимальная глубина воды

    private record Pending(ResourceKey<Level> dim, long chunk, long readyAt) {}

    /** Список водных чанков с O(1) случайным выбором и удалением. */
    private static final class Water {
        final ArrayList<Long> keys = new ArrayList<>();
        final HashMap<Long, Integer> index = new HashMap<>();
        final HashMap<Long, int[]> cols = new HashMap<>();

        void put(long k, int[] c) {
            if (!index.containsKey(k)) {
                index.put(k, keys.size());
                keys.add(k);
            }
            cols.put(k, c);
        }

        void remove(long k) {
            Integer i = index.remove(k);
            if (i == null) return;
            cols.remove(k);
            int last = keys.size() - 1;
            long moved = keys.get(last);
            keys.remove(last);
            if (i != last) {
                keys.set(i, moved);
                index.put(moved, i);
            }
        }
    }

    private static final Map<ResourceKey<Level>, Water> WATER = new HashMap<>();
    private static final ArrayDeque<Pending> QUEUE = new ArrayDeque<>();

    private FishSpawner() {}

    // ── учёт чанков ──
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        QUEUE.add(new Pending(level.dimension(), event.getChunk().getPos().toLong(),
                level.getGameTime() + SCAN_DELAY));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Water w = WATER.get(level.dimension());
        if (w != null) w.remove(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        WATER.clear();
        QUEUE.clear();
    }

    // ── тик ──
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();

        // сканирование новых загруженных чанков
        int n = SCAN_PER_TICK;
        while (n-- > 0 && !QUEUE.isEmpty()) {
            Pending p = QUEUE.peek();
            ServerLevel level = server.getLevel(p.dim());
            if (level == null) { QUEUE.poll(); continue; }
            if (level.getGameTime() < p.readyAt()) break;
            QUEUE.poll();

            ChunkPos cp = new ChunkPos(p.chunk());
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) continue;   // уже выгружен

            int[] cols = scan(level, cp);
            Water w = WATER.computeIfAbsent(p.dim(), k -> new Water());
            if (cols.length == 0) { w.remove(p.chunk()); continue; }
            w.put(p.chunk(), cols);
            populate(level, cp, cols, INIT_FILL, 1.0f);
        }

        // пополнение случайных водных чанков
        if (server.getTickCount() % REFILL_PERIOD != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            Water w = WATER.get(level.dimension());
            if (w == null || w.keys.isEmpty()) continue;
            RandomSource r = level.getRandom();
            for (int i = 0; i < REFILL_CHUNKS; i++) {
                long key = w.keys.get(r.nextInt(w.keys.size()));
                ChunkPos cp = new ChunkPos(key);
                if (!level.isPositionEntityTicking(cp.getMiddleBlockPosition(level.getSeaLevel()))) continue;
                int[] cols = w.cols.get(key);
                if (cols == null || cols.length == 0) continue;
                populate(level, cp, cols, REFILL_MAX, REFILL_CHANCE);
            }
        }
    }

    // ── сканирование ──
    /** Выбирает по одной точке в каждой клетке 4×4 блока и оставляет те, где достаточно воды. */
    private static int[] scan(ServerLevel level, ChunkPos cp) {
        RandomSource r = level.getRandom();
        int[] tmp = new int[16];
        int c = 0;
        for (int gx = 0; gx < 4; gx++) {
            for (int gz = 0; gz < 4; gz++) {
                int lx = gx * 4 + r.nextInt(4);
                int lz = gz * 4 + r.nextInt(4);
                if (validWater(level, cp.getMinBlockX() + lx, cp.getMinBlockZ() + lz)) {
                    tmp[c++] = (lx << 4) | lz;
                }
            }
        }
        return Arrays.copyOf(tmp, c);
    }

    private static boolean validWater(ServerLevel level, int x, int z) {
        int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        int topY   = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (topY - floorY < MIN_DEPTH) return false;
        BlockPos floor = new BlockPos(x, floorY, z);
        return level.getFluidState(floor).is(FluidTags.WATER)
                && level.getFluidState(floor.above()).is(FluidTags.WATER);
    }

    // ── заселение ──
    private static void populate(ServerLevel level, ChunkPos cp, int[] cols, int max, float chance) {
        RandomSource r = level.getRandom();
        if (r.nextFloat() > chance) return;

        AABB area = new AABB(cp.getMinBlockX() - 8, level.getMinBuildHeight(), cp.getMinBlockZ() - 8,
                cp.getMaxBlockX() + 9, level.getMaxBuildHeight(), cp.getMaxBlockZ() + 9);
        int have = level.getEntitiesOfClass(AbstractFish.class, area).size();
        int spawned = 0;

        for (int i = 0; i < TRIES && spawned < max && have + spawned < CHUNK_CAP; i++) {
            int c = cols[r.nextInt(cols.length)];
            int x = cp.getMinBlockX() + (c >> 4);
            int z = cp.getMinBlockZ() + (c & 15);
            if (!validWater(level, x, z)) continue;

            BlockPos floor = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
            BlockState st = level.getBlockState(floor);
            boolean kelp  = isKelp(st);
            boolean grass = st.is(Blocks.SEAGRASS) || st.is(Blocks.TALL_SEAGRASS);
            boolean river = level.getBiome(floor).is(BiomeTags.IS_RIVER);

            // на голой воде реже: растения предпочтительнее
            if (!kelp && !grass && r.nextFloat() > 0.3f) continue;

            EntityType<? extends AbstractFish> type = pickFish(kelp, grass, river, r);

            // поднимаем над дном на 1–3 блока, пока вокруг вода
            BlockPos at = floor;
            int lift = 1 + r.nextInt(3);
            for (int k = 0; k < lift; k++) {
                BlockPos up = at.above();
                if (!level.getFluidState(up).is(FluidTags.WATER)) break;
                at = up;
            }

            AbstractFish f = type.spawn(level, at, MobSpawnType.EVENT);
            if (f != null) {
                f.setPersistenceRequired();   // не исчезает вдали от игрока; число ограничено CHUNK_CAP
                spawned++;
            }
        }
    }

    private static boolean isKelp(BlockState st) {
        return st.is(Blocks.KELP) || st.is(Blocks.KELP_PLANT);
    }

    private static EntityType<? extends AbstractFish> pickFish(boolean kelp, boolean grass, boolean river, RandomSource r) {
        float f = r.nextFloat();
        if (kelp) {                                   // ламинарии: лосось и «экзотика»
            if (f < 0.45f) return EntityType.SALMON;
            if (f < 0.70f) return EntityType.TROPICAL_FISH;
            if (f < 0.90f) return EntityType.PUFFERFISH;
            return EntityType.COD;
        }
        if (grass) {                                  // трава: в основном треска
            if (f < 0.70f) return EntityType.COD;
            if (f < 0.85f) return EntityType.SALMON;
            if (f < 0.95f) return EntityType.TROPICAL_FISH;
            return EntityType.PUFFERFISH;
        }
        if (river) {                                  // голая вода в реке
            return f < 0.55f ? EntityType.SALMON : EntityType.COD;
        }
        // голая вода в океане
        if (f < 0.60f) return EntityType.COD;
        if (f < 0.85f) return EntityType.SALMON;
        return EntityType.TROPICAL_FISH;
    }
}