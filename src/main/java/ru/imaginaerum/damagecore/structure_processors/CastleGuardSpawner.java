package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CastleGuardSpawner {
    private static final ResourceKey<Structure> CASTLE = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("damagecore", "pillager_castle"));

    private static final int INTERVAL_TICKS = 20 * 60; // 1 минута (для теста; потом верните 3 минуты)
    private static final int MIN_ALIVE      = 6;
    private static final int GROUP_MIN      = 3;
    private static final int GROUP_MAX      = 6;
    private static final double MIN_PLAYER_DIST = 12.0;
    private static final boolean DEBUG = true;

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL_TICKS != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            Structure structure = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE).get(CASTLE);
            if (structure == null) {
                if (DEBUG) System.out.println("[CastleGuardSpawner] Структура damagecore:pillager_castle не найдена!");
                continue;
            }

            Set<ChunkPos> done = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                StructureStart start = level.structureManager()
                        .getStructureAt(player.blockPosition(), structure);
                if (!start.isValid()) continue;
                if (!done.add(start.getChunkPos())) continue;

                if (DEBUG) System.out.println("[CastleGuardSpawner] Обработка " + start.getChunkPos());

                // Точки охраны (железные блоки), записанные процессором
                Set<BlockPos> guardPoints = new HashSet<>();
                for (BlockPos p : CastleGuardData.get(level).getPoints()) {
                    if (start.getBoundingBox().isInside(p)) guardPoints.add(p);
                }
                if (DEBUG) System.out.println("[CastleGuardSpawner] Guard points: " + guardPoints.size());

                spawnAll(level, start, guardPoints, EntityType.PILLAGER,  Pillager.class);
                spawnAll(level, start, guardPoints, EntityType.VINDICATOR, Vindicator.class);
            }
        }
    }

    private static void spawnAll(ServerLevel level, StructureStart start, Set<BlockPos> guardPoints,
                                 EntityType<? extends Raider> type, Class<? extends Raider> clazz) {
        AABB area = AABB.of(start.getBoundingBox()).inflate(16);
        int alive = level.getEntitiesOfClass(clazz, area).size();
        if (DEBUG) System.out.println("[CastleGuardSpawner] " + type.getDescription().getString()
                + " alive=" + alive);

        if (alive >= MIN_ALIVE) return;

        int count = GROUP_MIN + level.random.nextInt(GROUP_MAX - GROUP_MIN + 1);
        count = Math.min(count, MIN_ALIVE * 2 - alive);

        int spawned = 0;
        if (!guardPoints.isEmpty()) {
            List<BlockPos> list = new ArrayList<>(guardPoints);
            for (int i = 0; i < count; i++) {
                BlockPos gp = list.get(level.random.nextInt(list.size()));
                if (spawnAroundPoint(level, type, gp)) spawned++;
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (spawnInPieces(level, start, type)) spawned++;
            }
        }
        if (DEBUG) System.out.println("[CastleGuardSpawner] Заспавнено " + spawned + "/" + count
                + " " + type.getDescription().getString());
    }

    private static boolean spawnAroundPoint(ServerLevel level, EntityType<? extends Raider> type,
                                            BlockPos center) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = random.nextInt(9) - 4;
            int dz = random.nextInt(9) - 4;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;

            Vec3 spot = findFloor(level, type, x, z, center.getY() - 4, center.getY() + 6);
            if (spot == null) continue;
            if (level.getNearestPlayer(spot.x, spot.y, spot.z, MIN_PLAYER_DIST, false) != null) continue;

            return doSpawn(level, type, spot, random);
        }
        return false;
    }

    private static boolean spawnInPieces(ServerLevel level, StructureStart start,
                                         EntityType<? extends Raider> type) {
        RandomSource random = level.random;
        List<StructurePiece> pieces = start.getPieces();
        if (pieces.isEmpty()) return false;

        for (int attempt = 0; attempt < 40; attempt++) {
            BoundingBox box = pieces.get(random.nextInt(pieces.size())).getBoundingBox();
            int x = box.minX() + random.nextInt(box.getXSpan());
            int z = box.minZ() + random.nextInt(box.getZSpan());

            Vec3 spot = findFloor(level, type, x, z, box.minY(), box.maxY());
            if (spot == null) continue;
            if (level.getNearestPlayer(spot.x, spot.y, spot.z, MIN_PLAYER_DIST, false) != null) continue;

            return doSpawn(level, type, spot, random);
        }
        return false;
    }

    private static boolean doSpawn(ServerLevel level, EntityType<? extends Raider> type,
                                   Vec3 spot, RandomSource random) {
        Raider mob = type.create(level);
        if (mob == null) {
            if (DEBUG) System.out.println("[CastleGuardSpawner] type.create() == null");
            return false;
        }

        mob.moveTo(spot.x, spot.y, spot.z, random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level,
                level.getCurrentDifficultyAt(BlockPos.containing(spot)),
                MobSpawnType.EVENT, null);
        mob.setCanJoinRaid(false);
        mob.setPersistenceRequired();

        // ВАЖНО: выдаём экипировку ПОСЛЕ finalizeSpawn, чтобы ванильный loot не перетёр её
        applyVariant(mob, random);

        if (!mob.isAlive()) {
            if (DEBUG) System.out.println("[CastleGuardSpawner] mob мёртв после finalizeSpawn");
            return false;
        }

        level.addFreshEntityWithPassengers(mob);
        boolean added = mob.isAlive() && level.getEntity(mob.getId()) != null;

        if (DEBUG) {
            System.out.println("[CastleGuardSpawner] + " + type.getDescription().getString()
                    + " @ " + spot + " ok=" + added
                    + " mainhand=" + mob.getItemBySlot(EquipmentSlot.MAINHAND));
        }
        return added;
    }

    private static Vec3 findFloor(ServerLevel level, EntityType<?> type,
                                  int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getBlockState(feet.below()).isSolid()) continue;
            double cx = x + 0.5, cz = z + 0.5;
            if (level.noCollision(type.getSpawnAABB(cx, y, cz))) {
                return new Vec3(cx, y, cz);
            }
        }
        return null;
    }

    private static void applyVariant(Mob mob, RandomSource random) {
        if (mob instanceof Vindicator) {
            float w = random.nextFloat();
            Item axe = w < 0.15f ? Items.DIAMOND_AXE
                    : w < 0.45f ? Items.STONE_AXE
                    : Items.IRON_AXE;
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(axe));
        } else if (mob instanceof Pillager) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        }

        float r = random.nextFloat();
        if (r < 0.25f) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        } else if (r < 0.40f) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        } else if (r < 0.50f) {
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.05f);
        }
    }
}