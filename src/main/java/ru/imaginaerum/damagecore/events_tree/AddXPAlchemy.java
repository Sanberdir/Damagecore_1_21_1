package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerRegistry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class AddXPAlchemy {

    private static final Map<UUID, Integer> fillCount = new HashMap<>(); // UUID вместо Player
    private static final float HP_PER_LEARNED_NODE = 1.0f;

    @SubscribeEvent
    public static void onPotionTaken(PlayerBrewedPotionEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack stack = event.getStack();
        if (stack == null || stack.isEmpty()) return;

        int alchemyTreeId = findAlchemyTreeId();
        if (alchemyTreeId == -1) return;

        // Добавляем XP
        SkillTreeXpManager.addXp(serverPlayer, alchemyTreeId, 1);

        UUID uuid = serverPlayer.getUUID();
        int count = fillCount.getOrDefault(uuid, 0) + 1;
        fillCount.put(uuid, count);

        // Лечение за изученные ноды
        Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(alchemyTreeId);
        if (nodes != null && !nodes.isEmpty()) {
            float totalHeal = 0f;
            for (Map.Entry<String, SkillTreeNode> e : nodes.entrySet()) {
                float progress = SkillTreeServerHandler.getNodeProgress(serverPlayer, alchemyTreeId, e.getKey());
                if (progress > 0f) totalHeal += progress * HP_PER_LEARNED_NODE;
            }
            if (totalHeal > 0f) {
                serverPlayer.heal(totalHeal);
            }
        }

        if (count >= 3) fillCount.put(uuid, 0);
    }

    private static int findAlchemyTreeId() {
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
            if (nodes == null) continue;
            for (String nodeId : nodes.keySet()) {
                if (nodeId != null && nodeId.toLowerCase(Locale.ROOT).contains("alchemy")) {
                    return treeId;
                }
            }
        }
        return -1;
    }
}