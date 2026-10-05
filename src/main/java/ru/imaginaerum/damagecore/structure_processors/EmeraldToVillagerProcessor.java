package ru.imaginaerum.damagecore.structure_processors; // укажите ваш пакет

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.client.CaptiveData;

public class EmeraldToVillagerProcessor extends StructureProcessor {
    // Кодек, возвращающий синглтон-экземпляр этого процессора
    public static final StructureProcessorType<EmeraldToVillagerProcessor> TYPE = () -> MapCodec.unit(new EmeraldToVillagerProcessor());

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos pos, BlockPos pivot,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings) {

        boolean emerald = relativeBlockInfo.state().is(Blocks.EMERALD_BLOCK);
        boolean guardPoint = relativeBlockInfo.state().is(Blocks.IRON_BLOCK);
        if (!emerald && !guardPoint) return relativeBlockInfo;

        BlockPos worldPos = relativeBlockInfo.pos();
        BoundingBox box = settings.getBoundingBox();

        if (box != null && box.isInside(worldPos) && level instanceof WorldGenLevel wgl) {
            if (emerald) {
                Villager villager = EntityType.VILLAGER.create(wgl.getLevel());
                if (villager != null) {
                    villager.moveTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5, 0f, 0f);
                    villager.finalizeSpawn(wgl, wgl.getCurrentDifficultyAt(worldPos), MobSpawnType.STRUCTURE, null);
                    villager.setPersistenceRequired();
                    villager.addTag(RescuableVillager.TAG);
                    wgl.addFreshEntityWithPassengers(villager);
                    CaptiveData.get(wgl.getLevel()).add(villager.getUUID(), worldPos);
                }
            } else {
                ServerLevel serverLevel = wgl.getLevel();
                CastleGuardData.get(serverLevel).addPoint(worldPos);
            }
        }
        return new StructureTemplate.StructureBlockInfo(worldPos, Blocks.AIR.defaultBlockState(), null);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }
}
