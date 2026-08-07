package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillTreeXpManager {
    private static final Map<ServerPlayer, Map<Integer, Integer>> playerTreeXp = new ConcurrentHashMap<>();
    private static final Map<ServerPlayer, Map<Integer, Integer>> playerTreeLevel = new ConcurrentHashMap<>();

    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final String XP_KEY = "tree_xp";
    private static final String LEVEL_KEY = "tree_level";

    private static final int BASE_XP_PER_LEVEL = 3;
    private static final double XP_GROWTH_FACTOR = 1.5;

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ДАННЫХ =====

    /**
     * Получить XP игрока для указанного дерева
     */
    public static int getXp(ServerPlayer player, int treeId) {
        Map<Integer, Integer> xpMap = playerTreeXp.get(player);
        if (xpMap == null) return 0;
        return xpMap.getOrDefault(treeId, 0);
    }

    /**
     * Получить уровень игрока для указанного дерева
     */
    public static int getLevel(ServerPlayer player, int treeId) {
        Map<Integer, Integer> levelMap = playerTreeLevel.get(player);
        if (levelMap == null) return 0;
        return levelMap.getOrDefault(treeId, 0);
    }

    /**
     * Получить прогресс к следующему уровню (0.0 - 1.0)
     */
    public static float getProgress(ServerPlayer player, int treeId) {
        int currentLevel = getLevel(player, treeId);
        int currentXp = getXp(player, treeId);
        int xpRequired = getXpRequiredForLevel(currentLevel);

        if (xpRequired <= 0) return 0f;
        return (float) currentXp / xpRequired;
    }

    /**
     * Получить общий XP до следующего уровня
     */
    public static int getXpToNextLevel(ServerPlayer player, int treeId) {
        int currentLevel = getLevel(player, treeId);
        int currentXp = getXp(player, treeId);
        int xpRequired = getXpRequiredForLevel(currentLevel);

        return Math.max(0, xpRequired - currentXp);
    }

    /**
     * Проверить, может ли игрок повысить уровень дерева
     */
    public static boolean canLevelUp(ServerPlayer player, int treeId) {
        int currentLevel = getLevel(player, treeId);
        int currentXp = getXp(player, treeId);
        int xpRequired = getXpRequiredForLevel(currentLevel);

        return currentXp >= xpRequired;
    }

    // ===== СУЩЕСТВУЮЩИЕ МЕТОДЫ =====

    public static void addXp(ServerPlayer player, int treeId, int amount) {
        Map<Integer, Integer> xpMap = playerTreeXp.computeIfAbsent(player, k -> new HashMap<>());
        Map<Integer, Integer> levelMap = playerTreeLevel.computeIfAbsent(player, k -> new HashMap<>());

        int currentXp = xpMap.getOrDefault(treeId, 0) + amount;
        int currentLevel = levelMap.getOrDefault(treeId, 0);

        // Повышаем уровень пока хватает XP
        int xpRequired = getXpRequiredForLevel(currentLevel);

        while (currentXp >= xpRequired) {
            currentXp -= xpRequired;
            currentLevel++;
            xpRequired = getXpRequiredForLevel(currentLevel);


            // Пересчитываем разблокировку нод при повышении уровня дерева
            final int finalLevel = currentLevel;
            Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
            if (nodes != null) {
                Map<String, Integer> nodeLevels = getNBTNodeLevels(player, treeId);
                for (SkillTreeNode node : nodes.values()) {
                    if (!node.locked) continue;
                    if (node.getRequiredTreeLevel() > finalLevel) continue;

                    boolean allParentsLearned = true;
                    for (String parentId : node.parentIds) {
                        if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;
                        if (nodeLevels.getOrDefault(parentId, 0) <= 0) {
                            allParentsLearned = false;
                            break;
                        }
                    }
                    if (allParentsLearned) {
                        node.locked = false;
                    }
                }
            }
        }

        xpMap.put(treeId, currentXp);
        levelMap.put(treeId, currentLevel);

        // Сохраняем в persistentData
        saveToPersistentData(player, treeId, currentXp, currentLevel);

        // Отправляем обновление клиенту
        syncToClient(player);

    }
    private static Map<String, Integer> getNBTNodeLevels(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        CompoundTag treeTag = mod.getCompound("tree_" + treeId);

        Map<String, Integer> result = new HashMap<>();
        for (String key : treeTag.getAllKeys()) {
            if (key.startsWith("node_level_")) {
                String nodeId = key.substring("node_level_".length());
                int lvl = treeTag.getInt(key);
                if (lvl > 0) result.put(nodeId, lvl);
            }
        }
        return result;
    }
    private static int getXpRequiredForLevel(int level) {
        if (level < 0) return 0;
        return (int) Math.floor(BASE_XP_PER_LEVEL * Math.pow(XP_GROWTH_FACTOR, level));
    }

    public static void loadFromPersistentData(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mod = persisted.getCompound(ROOT_KEY);

        Map<Integer, Integer> xpMap = new HashMap<>();
        Map<Integer, Integer> levelMap = new HashMap<>();

        if (mod.contains(XP_KEY)) {
            CompoundTag xpTag = mod.getCompound(XP_KEY);
            for (String key : xpTag.getAllKeys()) {
                try {
                    xpMap.put(Integer.parseInt(key), xpTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (mod.contains(LEVEL_KEY)) {
            CompoundTag levelTag = mod.getCompound(LEVEL_KEY);
            for (String key : levelTag.getAllKeys()) {
                try {
                    levelMap.put(Integer.parseInt(key), levelTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        playerTreeXp.put(player, xpMap);
        playerTreeLevel.put(player, levelMap);


        // Отправляем клиенту
        syncToClient(player);
    }

    private static void saveToPersistentData(ServerPlayer player, int treeId, int xp, int level) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted == null) persisted = new CompoundTag();

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        if (mod == null) mod = new CompoundTag();

        CompoundTag xpTag = mod.contains(XP_KEY) ? mod.getCompound(XP_KEY) : new CompoundTag();
        CompoundTag levelTag = mod.contains(LEVEL_KEY) ? mod.getCompound(LEVEL_KEY) : new CompoundTag();

        xpTag.putInt(String.valueOf(treeId), xp);
        levelTag.putInt(String.valueOf(treeId), level);

        mod.put(XP_KEY, xpTag);
        mod.put(LEVEL_KEY, levelTag);
        persisted.put(ROOT_KEY, mod);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void syncToClient(ServerPlayer player) {
        Map<Integer, Integer> xpMap = playerTreeXp.getOrDefault(player, new HashMap<>());
        Map<Integer, Integer> levelMap = playerTreeLevel.getOrDefault(player, new HashMap<>());

        // ИСПРАВЛЕНО: Современный и безопасный синтаксис отправки пакета конкретному игроку
        PacketDistributor.sendToPlayer(player, new SyncTreeXpPacket(xpMap, levelMap));
    }

    public static void removePlayer(ServerPlayer player) {
        playerTreeXp.remove(player);
        playerTreeLevel.remove(player);

    }
}