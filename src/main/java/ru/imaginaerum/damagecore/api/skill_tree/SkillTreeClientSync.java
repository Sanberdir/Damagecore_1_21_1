package ru.imaginaerum.damagecore.api.skill_tree;


import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.Render;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side synchronization helpers for skill tree progress:
 * - levelCache: treeId -> (nodeId -> level)
 * - variantCache: treeId -> (nodeId -> selectedOption)
 *
 * Supports:
 * - applyLearnedNodes(treeId, List<String>) — compatibility with old "learned list" packets:
 *      converts every id -> level 1 and applies.
 * - applyNodeLevels(treeId, Map<String,Integer>) — apply arbitrary node levels.
 * - applyVariants(treeId, Map<String,Integer>) — apply selected variants.
 * - reapplyCachedForAllTrees() — reapply caches after trees are loaded client-side.
 */
public final class SkillTreeClientSync {
    private SkillTreeClientSync() {}

    // treeId -> (nodeId -> level)
    private static final Map<Integer, Map<String, Integer>> levelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<String, Integer>> cachedLevels = new HashMap<>();
    private static final Map<Integer, Map<String, Integer>> cachedVariants = new HashMap<>();
    // treeId -> (nodeId -> selectedOption)
    private static final Map<Integer, Map<String, Integer>> variantCache = new ConcurrentHashMap<>();

    private static Field treesField = null;

    // Reflection helper: get SkillTreeRenderer.trees map
    private static Map<?,?> getTreesMap() {
        try {
            if (treesField == null) {
                Class<?> rendererCls = Class.forName("ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer");
                treesField = rendererCls.getDeclaredField("trees");
                treesField.setAccessible(true);
            }
            Object obj = treesField.get(null);
            if (obj instanceof Map<?,?> map) return map;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Collections.emptyMap();
    }
    public static void clearCache() {
        levelCache.clear();
        variantCache.clear();
        cachedLevels.clear();
        cachedVariants.clear();

        Map<?, ?> trees = getTreesMap();
        if (trees == null || trees.isEmpty()) {
            return;
        }

        for (Object treeObj : trees.values()) {
            try {
                Field nodesField = treeObj.getClass().getDeclaredField("nodes");
                nodesField.setAccessible(true);
                Object nodesObj = nodesField.get(treeObj);
                if (!(nodesObj instanceof Map<?, ?> nodesMap)) continue;

                for (Object nodeObj : nodesMap.values()) {
                    if (!(nodeObj instanceof SkillTreeNode node)) continue;
                    node.level = 0;
                    node.selectedOption = -1;
                    node.locked = true;
                    node.blockedByTreeLevel = false;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

    /** Public getter for level cache for a tree (may return null) */
    public static Map<String, Integer> getLevelCache(int treeId) {
        return levelCache.get(treeId);
    }

    /**
     * Backwards-compatible: apply list of learned node ids (from older server packet).
     * Each id becomes level = 1 in the level cache.
     */
    public static void applyLearnedNodes(int treeId, List<String> learnedIds) {
        if (learnedIds == null) return;
        Map<String,Integer> map = new HashMap<>();
        for (String id : learnedIds) {
            if (id != null) map.put(id, 1);
        }
        applyNodeLevels(treeId, map);
    }

    /**
     * Apply node levels for a tree and immediately apply to any loaded client-side tree.
     * Levels are clamped to the node.maxLevel when the node exists.
     */
    public static void applyNodeLevels(int treeId, Map<String, Integer> levels) {
        if (levels == null) return;

        // update cache (replace)
        levelCache.put(treeId, new HashMap<>(levels));

        Object treeObj = getTreesMap().get(treeId);
        if (treeObj == null) {
            // tree not loaded yet, will be applied when tree loads
            return;
        }

        try {
            Field nodesField = treeObj.getClass().getDeclaredField("nodes");
            nodesField.setAccessible(true);
            Object nodesObj = nodesField.get(treeObj);
            if (!(nodesObj instanceof Map<?, ?> nodesMap)) return;

            boolean changed = false;

            // 1) Apply levels from cache (clamped to node.maxLevel if node present)
            for (Map.Entry<String,Integer> e : levels.entrySet()) {
                String nid = e.getKey();
                int lvl = (e.getValue() == null) ? 0 : e.getValue();
                Object nodeObj = nodesMap.get(nid);
                if (nodeObj instanceof SkillTreeNode) {
                    SkillTreeNode node = (SkillTreeNode) nodeObj;
                    int clamped = Math.max(0, Math.min(node.maxLevel, lvl));
                    if (node.level != clamped) {
                        node.level = clamped;
                        // if gained level >=1, ensure not locked
                        if (node.level > 0 && node.locked) node.locked = false;
                        changed = true;
                    }
                }
            }

            // 2) For nodes not in incoming map, we shouldn't overwrite local values.
            // But we must recalc locks for all nodes based on parents' levels (>=1 required).
            for (Object entryObj : nodesMap.values()) {
                if (!(entryObj instanceof SkillTreeNode)) continue;
                SkillTreeNode node = (SkillTreeNode) entryObj;

                boolean shouldBeLocked;

                if (node.isRoot()) {
                    shouldBeLocked = false;
                } else {
                    boolean allParentsAtLeastOne = true;

                    for (String pid : node.parentIds) {
                        if (pid == null || "start".equalsIgnoreCase(pid)) continue;

                        Map<String, Integer> cached = levelCache.get(treeId);

                        int pLevel = 0;

                        if (cached != null && cached.containsKey(pid)) {
                            pLevel = cached.get(pid);
                        } else {
                            SkillTreeNode parentNode = (SkillTreeNode) nodesMap.get(pid);
                            if (parentNode != null) {
                                pLevel = parentNode.level;
                            }
                        }

                        if (pLevel <= 0) {
                            allParentsAtLeastOne = false;
                            break;
                        }
                    }

                    shouldBeLocked = !allParentsAtLeastOne;
                }

// Проверка уровня дерева
                int clientTreeLevel = DamageBookRenderer.getLevel(treeId);
                if (node.getRequiredTreeLevel() > clientTreeLevel) {
                    shouldBeLocked = true;
                    node.blockedByTreeLevel = true;
                } else {
                    node.blockedByTreeLevel = false;
                }

// 💥 КЛЮЧЕВОЕ
                if (node.locked != shouldBeLocked) {
                    node.locked = shouldBeLocked;
                    changed = true;
                }
            }

            if (changed) {
                // request recalculation/render update
                Render.invokePrivateCalculateAndUpdatePositions(treeObj,
                        Render.currentPanelScreenX,
                        Render.currentPanelScreenY);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /**
     * Полная очистка всех кэшей при смене мира/выходе
     */
    public static void clearAllCaches() {
        levelCache.clear();
        variantCache.clear();

        // Сброс состояния узлов в загруженных деревьях
        Map<?,?> trees = getTreesMap();
        for (Object treeObj : trees.values()) {
            try {
                Field nodesField = treeObj.getClass().getDeclaredField("nodes");
                nodesField.setAccessible(true);
                Map<String, SkillTreeNode> nodes = (Map<String, SkillTreeNode>) nodesField.get(treeObj);

                for (SkillTreeNode node : nodes.values()) {
                    node.level = 0;
                    node.locked = true; // По умолчанию заблокировано
                    node.selectedOption = -1;
                    node.blockedByTreeLevel = false;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
    /**
     * Apply variants (node selected options) for a specific tree.
     */
    public static void applyVariants(int treeId, Map<String, Integer> variants) {
        if (variants == null || variants.isEmpty()) return;

        variantCache.put(treeId, new HashMap<>(variants));

        Object treeObj = getTreesMap().get(treeId);
        System.out.println("[Client] ApplyVariants tree=" + treeId + " " + variants);
        // если дерево уже есть — применяем сразу
        if (treeObj != null) {
            applyVariantsToTree(treeId);
        }
    }

    // internal: apply cached variants to a loaded tree
    private static void applyVariantsToTree(int treeId) {
        Map<String,Integer> variants = variantCache.get(treeId);
        if (variants == null || variants.isEmpty()) return;
        System.out.println("[Client] Applying to loaded tree " + treeId);
        Object treeObj = getTreesMap().get(treeId);
        if (treeObj == null) return;

        try {
            Field nodesField = treeObj.getClass().getDeclaredField("nodes");
            nodesField.setAccessible(true);
            Object nodesObj = nodesField.get(treeObj);
            if (!(nodesObj instanceof Map<?,?> nodesMap)) return;

            boolean changed = false;
            for (Map.Entry<String,Integer> e : variants.entrySet()) {
                String nid = e.getKey();
                Integer idx = e.getValue();
                Object nodeObj = nodesMap.get(nid);
                if (nodeObj instanceof SkillTreeNode) {
                    SkillTreeNode node = (SkillTreeNode) nodeObj;
                    int sel = (idx == null) ? -1 : idx;
                    if (node.selectedOption != sel) {
                        // safe apply (applyVariant checks bounds)
                        node.applyVariant(sel);
                        changed = true;
                    }
                }
            }

            if (changed) {
                Render.invokePrivateCalculateAndUpdatePositions(treeObj,
                        Render.currentPanelScreenX,
                        Render.currentPanelScreenY);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Reapply all cached data for all trees.
     * Called after SkillTreeRenderer.loadAllTrees()
     */
    public static void reapplyCachedForAllTrees() {
        Map<?, ?> trees = getTreesMap();
        if (trees == null || trees.isEmpty()) {
            return;
        }

        for (Object key : trees.keySet()) {
            if (!(key instanceof Integer treeId)) continue;


            Map<String, Integer> levels = levelCache.get(treeId);
            if (levels != null && !levels.isEmpty()) {
                applyNodeLevels(treeId, levels);
            }

            Map<String, Integer> variants = variantCache.get(treeId);
            if (variants != null && !variants.isEmpty()) {
                applyVariantsToTree(treeId);
            }
        }
    }

}