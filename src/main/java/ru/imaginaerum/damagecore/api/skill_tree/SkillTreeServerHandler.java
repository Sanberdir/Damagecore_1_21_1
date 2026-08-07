package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.skill_tree.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

import java.util.HashMap;
import java.util.Map;

public final class SkillTreeServerHandler {
    private SkillTreeServerHandler() {}

    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final int REQUIRED_LEVELS = 5;
    private static final String NODE_LEVEL_PREFIX = "node_level_";

    // --------------------------------------------------
    // Публичные методы для внешнего использования
    // --------------------------------------------------

    public static boolean isNodeLearned(ServerPlayer player, String nodeId) {
        if (player == null || nodeId == null) return false;
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            if (getNodeLevels(player, treeId).getOrDefault(nodeId, 0) > 0) return true;
        }
        return false;
    }

    public static float getNodeProgress(ServerPlayer player, int treeId, String nodeId) {
        if (player == null || nodeId == null) return 0f;
        SkillTreeNode node = SkillTreeServerRegistry.getNode(treeId, nodeId);
        if (node == null) return 0f;
        int currentLevel = getNodeLevels(player, treeId).getOrDefault(nodeId, 0);
        return Math.min(1f, (float) currentLevel / Math.max(1, node.maxLevel));
    }

    public static SkillTreeNode getNodeForPlayer(ServerPlayer player, String nodeId) {
        if (nodeId == null) return null;
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            SkillTreeNode node = SkillTreeServerRegistry.getNode(treeId, nodeId);
            if (node != null) return node;
        }
        return null;
    }

    // --------------------------------------------------
    // Основная логика изучения ноды
    // --------------------------------------------------

    public static void handleLearnRequest(ServerPlayer player, int treeId, String nodeId) {
        Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
        if (nodes == null) return;

        SkillTreeNode node = nodes.get(nodeId);
        if (node == null) return;

        // проверка родителей
        if (!canLearn(player, treeId, node)) return;

        // получаем текущие уровни игрока
        Map<String, Integer> levels = getNodeLevels(player, treeId);

        int currentLevel = levels.getOrDefault(node.id, 0);
        int newLevel = Math.min(node.maxLevel, currentLevel + 1);

        // сохраняем
        saveNodeLevel(player, treeId, node.id, newLevel);
        player.level().playSound(
                null, // null = слышат все рядом
                player.getX(),
                player.getY(),
                player.getZ(),
                DCSoundEvents.LEARNING_SKILL.get(), // проверь имя!
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );
        // синк
        sendFullSyncToPlayer(player);
    }
    private static boolean canLearn(ServerPlayer player, int treeId, SkillTreeNode node) {
        if (node.isRoot()) return true;

        Map<String, Integer> levels = getNodeLevels(player, treeId);

        for (String pid : node.parentIds) {
            if (pid == null || "start".equalsIgnoreCase(pid)) continue;

            int lvl = levels.getOrDefault(pid, 0);
            if (lvl <= 0) return false;
        }

        return true;
    }
    // --------------------------------------------------
    // Сохранение варианта ноды
    // --------------------------------------------------

    public static void saveNodeVariant(ServerPlayer player, SkillTreeNode node, int treeId) {
        if (player == null || node == null) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        treeTag.putInt("node_variant_" + node.id, node.selectedOption);

        mod.put("tree_" + treeId, treeTag);
        persisted.put(ROOT_KEY, mod);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        System.out.println("[Server] Saved variant: " + node.id + " = " + node.selectedOption);
    }

    // --------------------------------------------------
    // Полная синхронизация при входе игрока
    // --------------------------------------------------

    public static void sendFullSyncToPlayer(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        if (mod.isEmpty()) return;

        for (String key : mod.getAllKeys()) {
            if (!key.startsWith("tree_")) continue;
            try {
                int treeId = Integer.parseInt(key.substring(5));
                CompoundTag treeTag = mod.getCompound(key);

                // Уровни нод
                Map<String, Integer> levels = new HashMap<>();
                for (String tk : treeTag.getAllKeys()) {
                    if (tk.startsWith(NODE_LEVEL_PREFIX)) {
                        String nid = tk.substring(NODE_LEVEL_PREFIX.length());
                        int lvl = treeTag.getInt(tk);
                        if (lvl > 0) levels.put(nid, lvl);
                    }
                }

                if (!levels.isEmpty()) {
                    // ИСПРАВЛЕНО: Современный синтаксис отправки пакета игроку в NeoForge 1.21.1
                    PacketDistributor.sendToPlayer(
                            player,
                            new SyncNodeLevelsPacket(treeId, levels)
                    );
                }

                // Варианты нод
                Map<String, Integer> variants = new HashMap<>();
                for (String varKey : treeTag.getAllKeys()) {
                    if (varKey.startsWith("node_variant_")) {
                        String nid = varKey.substring("node_variant_".length());
                        variants.put(nid, treeTag.getInt(varKey));
                    }
                }

                Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
                for (Map.Entry<String, Integer> entry : variants.entrySet()) {
                    SkillTreeNode node = nodes.get(entry.getKey());
                    if (node != null) node.applyVariant(entry.getValue());
                }

                if (!variants.isEmpty()) {
                    // ИСПРАВЛЕНО: Современный синтаксис отправки пакета игроку в NeoForge 1.21.1
                    PacketDistributor.sendToPlayer(
                            player,
                            new SyncNodeVariantsPacket(treeId, variants)
                    );
                }

            } catch (NumberFormatException ignored) {}
        }
    }

    // --------------------------------------------------
    // NBT helpers
    // --------------------------------------------------
    public static int getNodeLevel(ServerPlayer player, String nodeId) {
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            int lvl = getNodeLevels(player, treeId).getOrDefault(nodeId, 0);
            if (lvl > 0) return lvl;
        }
        return 0;
    }
    private static Map<String, Integer> getNodeLevels(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        Map<String, Integer> result = new HashMap<>();
        for (String key : treeTag.getAllKeys()) {
            if (key.startsWith(NODE_LEVEL_PREFIX)) {
                String nodeId = key.substring(NODE_LEVEL_PREFIX.length());
                int lvl = treeTag.getInt(key);
                if (lvl > 0) result.put(nodeId, lvl);
            }
        }
        return result;
    }

    private static void saveNodeLevel(ServerPlayer player, int treeId, String nodeId, int level) {
        if (player == null || nodeId == null) return;

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        treeTag.putInt(NODE_LEVEL_PREFIX + nodeId, Math.max(0, level));

        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}