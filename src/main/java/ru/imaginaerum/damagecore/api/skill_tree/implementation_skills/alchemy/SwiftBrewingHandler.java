package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerRegistry;
// Импортируйте ваш созданный интерфейс миксина
import ru.imaginaerum.damagecore.mixin.BrewingStandBlockEntityAccessor;

@EventBusSubscriber(modid = "damagecore")
public class SwiftBrewingHandler {

    private static final String NODE_ID = "swift_brewing";

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        // Проверяем, что тик происходит на стороне логического сервера
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        int treeId = -1;
        for (int id : SkillTreeServerRegistry.getAllTreeIds()) {
            if (SkillTreeServerRegistry.getNode(id, NODE_ID) != null) {
                treeId = id;
                break;
            }
        }
        if (treeId == -1) return;

        for (ServerPlayer player : serverLevel.players()) {
            int nodeLevel = SkillTreeServerHandler.getNodeLevel(player, NODE_ID); // уровень 0-3
            if (nodeLevel <= 0) continue;

            BlockPos playerPos = player.blockPosition();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        BlockPos checkPos = playerPos.offset(dx, dy, dz);
                        BlockEntity be = serverLevel.getBlockEntity(checkPos);

                        if (be instanceof BrewingStandBlockEntity brewingStand) {
                            accelerateBrewing(brewingStand, nodeLevel);
                        }
                    }
                }
            }
        }
    }

    private static void accelerateBrewing(BrewingStandBlockEntity brewingStand, int nodeLevel) {
        // Приводим блок к нашему интерфейсу-аксессору
        BrewingStandBlockEntityAccessor accessor = (BrewingStandBlockEntityAccessor) brewingStand;

        int brewTime = accessor.getBrewTime();
        if (brewTime > 0) {
            // Рассчитываем ускоренное время варки
            int newTime = Math.max(1, brewTime - nodeLevel);
            accessor.setBrewTime(newTime);

            // Обязательно помечаем блок как измененный для синхронизации
            brewingStand.setChanged();
        }
    }
}
