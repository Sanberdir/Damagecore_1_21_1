package ru.imaginaerum.damagecore.entity.chest_of_wild_currents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import ru.imaginaerum.damagecore.entity.ModEntities;

import static org.openjdk.nashorn.internal.objects.NativeMath.log;
import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

@EventBusSubscriber(modid = MODID)
public class SunkenBarrelSpawner {
    private static final int CHECK_INTERVAL = 40;
    private static final float CHANCE = 0.002f; // было 0.01f
    private static final int MIN_DIST = 24;
    private static final int MAX_DIST = 56;
    private static final int MIN_DEPTH = 3;
    private static final int MAX_NEARBY = 3;
    private static final int ATTEMPTS = 10;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.random.nextFloat() > CHANCE) continue;

            AABB area = player.getBoundingBox().inflate(MAX_DIST + 32);
            if (level.getEntitiesOfClass(SunkenBarrelEntity.class, area).size() >= MAX_NEARBY) continue;

            for (int i = 0; i < ATTEMPTS; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dist = MIN_DIST + level.random.nextDouble() * (MAX_DIST - MIN_DIST);
                int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
                int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);

                BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
                if (!level.isPositionEntityTicking(column)) { log("not ticking", column); continue; }

                // идём вниз от верха столба: пропускаем воздух, лёд и снег до первой воды
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(
                        x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                int steps = 0;
                while (steps++ < 6 && !level.getFluidState(p).is(FluidTags.WATER)) {
                    BlockState s = level.getBlockState(p);
                    if (!(s.isAir() || s.is(BlockTags.ICE) || s.is(BlockTags.SNOW))) break;
                    p.move(Direction.DOWN);
                }
                if (!level.getFluidState(p).is(FluidTags.WATER)) { log("no water", p); continue; }

                if (!level.getBiome(p).is(BiomeTags.IS_OCEAN)) {
                    log("biome " + level.getBiome(p).unwrapKey().map(k -> k.location().toString()).orElse("?"), p);
                    continue;
                }
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                int depth = p.getY() - floor + 1;
                if (depth < MIN_DEPTH) { log("shallow " + depth, p); continue; }

                SunkenBarrelEntity barrel = ModEntities.SUNKEN_BARREL.get().create(level);
                if (barrel == null) break;
                barrel.moveTo(x + 0.5, p.getY() + 0.5, z + 0.5, level.random.nextFloat() * 360f, 0f);
                level.addFreshEntity(barrel);
                log("SPAWNED", p);
                break;
            }
        }
    }
}