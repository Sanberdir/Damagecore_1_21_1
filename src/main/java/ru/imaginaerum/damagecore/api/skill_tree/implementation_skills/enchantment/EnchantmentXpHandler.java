package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.enchantment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerRegistry;
import ru.imaginaerum.damagecore.events_tree.SkillTreeXpManager;

@EventBusSubscriber(modid = "damagecore")
public class EnchantmentXpHandler {

    private static final String NODE_ID = "start_enchantment";
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final String NODE_LEVEL_PREFIX = "node_level_";
    private static final int XP_REWARD = 5;

    @SubscribeEvent
    public static void onGrindstoneTake(GrindstoneEvent.OnTakeItem event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.getXp() <= 0) return;

        int treeId = findTreeId(NODE_ID);
        if (treeId < 0) return; // такой ноды нет ни в одном дереве

        if (!hasNode(player, treeId)) return;

        SkillTreeXpManager.addXp(player, treeId, XP_REWARD);
    }

    /** Ищет дерево, содержащее ноду. -1, если не найдено. */
    private static int findTreeId(String nodeId) {
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            if (SkillTreeServerRegistry.getNode(treeId, nodeId) != null) return treeId;
        }
        return -1;
    }

    private static boolean hasNode(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tree = persisted.getCompound(ROOT_KEY).getCompound("tree_" + treeId);
        return tree.getInt(NODE_LEVEL_PREFIX + NODE_ID) > 0;
    }
}