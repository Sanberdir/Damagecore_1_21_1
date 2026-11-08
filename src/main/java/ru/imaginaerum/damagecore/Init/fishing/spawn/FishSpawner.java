
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

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class FishSpawner {

    // ── Настройки популяции ──

    static final int INIT_FILL = 8;
    static final int CHUNK_CAP = 16;
    static final int REFILL_THRESHOLD = 4;

    // Постепенное восстановление популяции
    static final int REFILL_CHUNKS = 16;
    static final int REFILL_MAX = 2;
    static final int REFILL_PERIOD = 20;

    // Минимальная дистанция от игрока
    static final double MIN_PLAYER_DISTANCE = 24.0;

    // Попытки поиска подходящего места
    static final int TRIES = 96;
    static final int MIN_DEPTH = 1;

    // Сканирование новых чанков
    static final int SCAN_DELAY = 40;
    static final int SCAN_PER_TICK = 6;

    private record Pending(
            ResourceKey<Level> dim,
            long chunk,
            long readyAt
    ) {}

    private static final class Water {

        final ArrayList<Long> keys = new ArrayList<>();
        final HashMap<Long, Integer> index = new HashMap<>();
        final HashMap<Long, int[]> cols = new HashMap<>();

        // Чанки, в которых уже началось восстановление
        final HashSet<Long> refilling = new HashSet<>();

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
            refilling.remove(k);

            int last = keys.size() - 1;
            long moved = keys.get(last);

            keys.remove(last);

            if (i != last) {
                keys.set(i, moved);
                index.put(moved, i);
            }
        }
    }

    private static final Map<ResourceKey<Level>, Water> WATER =
            new HashMap<>();

    private static final ArrayDeque<Pending> QUEUE =
            new ArrayDeque<>();

    private FishSpawner() {}

    // ── Учёт чанков ──

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        QUEUE.add(new Pending(
                level.dimension(),
                event.getChunk().getPos().toLong(),
                level.getGameTime() + SCAN_DELAY
        ));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Water w = WATER.get(level.dimension());

        if (w != null) {
            w.remove(event.getChunk().getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        WATER.clear();
        QUEUE.clear();
    }

    // ── Серверный тик ──

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        var server = event.getServer();

        // Первичное сканирование чанков.
        // Не блокируем очередь, если чанк из другого измерения
        // ещё не достиг времени сканирования.

        int scanCount = Math.min(SCAN_PER_TICK, QUEUE.size());

        for (int i = 0; i < scanCount; i++) {

            Pending p = QUEUE.poll();
            if (p == null) break;

            ServerLevel level = server.getLevel(p.dim());

            if (level == null) continue;

            if (level.getGameTime() < p.readyAt()) {
                QUEUE.addLast(p);
                continue;
            }

            ChunkPos cp = new ChunkPos(p.chunk());

            if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
                continue;
            }

            int[] cols = scan(level, cp);

            Water w = WATER.computeIfAbsent(
                    p.dim(),
                    k -> new Water()
            );

            if (cols.length == 0) {
                w.remove(p.chunk());
                continue;
            }

            w.put(p.chunk(), cols);

            // Первоначальное заселение.
            populate(level, cp, cols, INIT_FILL);
        }

        // Восстановление популяции.
        if (server.getTickCount() % REFILL_PERIOD != 0) return;

        for (ServerLevel level : server.getAllLevels()) {

            Water w = WATER.get(level.dimension());

            if (w == null || w.keys.isEmpty()) continue;

            RandomSource random = level.getRandom();

            int total = w.keys.size();
            int count = Math.min(REFILL_CHUNKS, total);

            // Выбираем уникальные чанки без повторений.
            int[] indices = new int[total];

            for (int i = 0; i < total; i++) {
                indices[i] = i;
            }

            for (int i = 0; i < count; i++) {

                int j = i + random.nextInt(total - i);

                int temp = indices[i];
                indices[i] = indices[j];
                indices[j] = temp;

                long key = w.keys.get(indices[i]);
                ChunkPos cp = new ChunkPos(key);

                // Проверяем, что чанк всё ещё загружен.
                // Не используем isPositionEntityTicking:
                // оно может исключать подходящие водные чанки
                // из-за границ Simulation Distance.

                if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
                    continue;
                }

                int[] cols = w.cols.get(key);

                if (cols == null || cols.length == 0) {
                    continue;
                }

                int have = countFishInChunk(level, cp);

                // Начинаем восстановление при 4 рыбах или меньше.
                if (!w.refilling.contains(key)
                        && have <= REFILL_THRESHOLD) {

                    w.refilling.add(key);
                }

                if (!w.refilling.contains(key)) {
                    continue;
                }

                // Восстановление закончено.
                if (have >= CHUNK_CAP) {
                    w.refilling.remove(key);
                    continue;
                }

                populate(level, cp, cols, REFILL_MAX);
            }
        }
    }

    // ── Сканирование водных колонок ──

    private static int[] scan(ServerLevel level, ChunkPos cp) {

        int[] tmp = new int[256];
        int count = 0;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {

                int x = cp.getMinBlockX() + lx;
                int z = cp.getMinBlockZ() + lz;

                if (validWater(level, x, z)) {
                    tmp[count++] = (lx << 4) | lz;
                }
            }
        }

        return Arrays.copyOf(tmp, count);
    }

    private static boolean validWater(
            ServerLevel level,
            int x,
            int z
    ) {

        int floorY = level.getHeight(
                Heightmap.Types.OCEAN_FLOOR,
                x,
                z
        );

        int topY = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                x,
                z
        );

        if (topY - floorY < MIN_DEPTH) return false;

        BlockPos floor = new BlockPos(x, floorY, z);

        return level.getFluidState(floor).is(FluidTags.WATER)
                && level.getFluidState(floor.above()).is(FluidTags.WATER);
    }

    // ── Подсчёт популяции ──

    private static int countFishInChunk(
            ServerLevel level,
            ChunkPos cp
    ) {

        AABB area = new AABB(
                cp.getMinBlockX(),
                level.getMinBuildHeight(),
                cp.getMinBlockZ(),
                cp.getMaxBlockX() + 1,
                level.getMaxBuildHeight(),
                cp.getMaxBlockZ() + 1
        );

        return level.getEntitiesOfClass(
                AbstractFish.class,
                area
        ).size();
    }

    // ── Проверка дистанции от игроков ──

    private static boolean isFarFromPlayers(
            ServerLevel level,
            ChunkPos cp
    ) {

        double minX = cp.getMinBlockX();
        double maxX = cp.getMaxBlockX() + 1.0;

        double minZ = cp.getMinBlockZ();
        double maxZ = cp.getMaxBlockZ() + 1.0;

        double minDistanceSqr =
                MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE;

        for (var player : level.players()) {

            double dx = Math.max(
                    0.0,
                    Math.max(
                            minX - player.getX(),
                            player.getX() - maxX
                    )
            );

            double dz = Math.max(
                    0.0,
                    Math.max(
                            minZ - player.getZ(),
                            player.getZ() - maxZ
                    )
            );

            if (dx * dx + dz * dz < minDistanceSqr) {
                return false;
            }
        }

        return true;
    }

    // ── Заселение ──

    private static void populate(
            ServerLevel level,
            ChunkPos cp,
            int[] cols,
            int max
    ) {

        if (cols.length == 0 || max <= 0) return;

        // Не создаём рыб рядом с игроком.
        if (!isFarFromPlayers(level, cp)) return;

        int have = countFishInChunk(level, cp);

        int room = CHUNK_CAP - have;

        if (room <= 0) return;

        RandomSource random = level.getRandom();

        int limit = Math.min(max, room);
        int spawned = 0;

        for (int i = 0; i < TRIES && spawned < limit; i++) {

            int column = cols[random.nextInt(cols.length)];

            int x = cp.getMinBlockX() + (column >> 4);
            int z = cp.getMinBlockZ() + (column & 15);

            if (!validWater(level, x, z)) continue;

            BlockPos waterFloor = new BlockPos(
                    x,
                    level.getHeight(
                            Heightmap.Types.OCEAN_FLOOR,
                            x,
                            z
                    ),
                    z
            );

            boolean kelp = false;
            boolean grass = false;

            for (int y = 0; y < 3; y++) {

                BlockState state = level.getBlockState(
                        waterFloor.above(y)
                );

                if (isKelp(state)) {
                    kelp = true;
                }

                if (state.is(Blocks.SEAGRASS)
                        || state.is(Blocks.TALL_SEAGRASS)) {
                    grass = true;
                }
            }

            boolean river = level.getBiome(waterFloor)
                    .is(BiomeTags.IS_RIVER);

            boolean ocean = level.getBiome(waterFloor)
                    .is(BiomeTags.IS_OCEAN);

            EntityType<? extends AbstractFish> type =
                    pickFish(kelp, grass, river, ocean, random);

            // Выбираем случайную высоту в водной колонке.
            BlockPos spawnPos = waterFloor;

            int lift = 1 + random.nextInt(3);

            for (int k = 0; k < lift; k++) {

                BlockPos above = spawnPos.above();

                if (!level.getFluidState(above).is(FluidTags.WATER)) {
                    break;
                }

                spawnPos = above;
            }

            AbstractFish fish = type.spawn(
                    level,
                    spawnPos,
                    MobSpawnType.EVENT
            );

            if (fish != null) {
                spawned++;
            }
        }
    }

    // ── Растительность ──

    private static boolean isKelp(BlockState state) {

        return state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT);
    }

    // ── Выбор вида рыбы ──

    // ── Выбор вида рыбы ──

    private static EntityType<? extends AbstractFish> pickFish(
            boolean kelp,
            boolean grass,
            boolean river,
            boolean ocean,
            RandomSource random
    ) {

        float f = random.nextFloat();

        // 1. Реки — строго лосось (самое специфичное условие — первым).
        if (river) {
            return EntityType.SALMON;
        }

        // 2. Всё, что НЕ океан — только лосось.
        //    Никакой трески вне IS_OCEAN: болота, пляжи,
        //    модовые водные биомы и реки без тега IS_RIVER
        //    больше не получат треску.
        if (!ocean) {
            return EntityType.SALMON;
        }

        // 3. Дальше — только настоящий океан (IS_OCEAN == true).

        // Океан с ламинарией.
        if (kelp) {

            if (f < 0.40f) return EntityType.COD;
            if (f < 0.60f) return EntityType.SALMON;
            if (f < 0.85f) return EntityType.TROPICAL_FISH;

            return EntityType.PUFFERFISH;
        }

        // Океан с морской травой.
        if (grass) {

            if (f < 0.50f) return EntityType.COD;
            if (f < 0.65f) return EntityType.SALMON;
            if (f < 0.85f) return EntityType.TROPICAL_FISH;

            return EntityType.PUFFERFISH;
        }

        // Открытый океан.
        if (f < 0.45f) return EntityType.COD;
        if (f < 0.60f) return EntityType.SALMON;
        if (f < 0.85f) return EntityType.TROPICAL_FISH;

        return EntityType.PUFFERFISH;
    }
}
