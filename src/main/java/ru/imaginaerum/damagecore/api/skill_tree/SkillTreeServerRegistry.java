package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверное хранилище деревьев навыков.
 * Загружается при старте сервера, не зависит от клиентского кода.
 */
public final class SkillTreeServerRegistry {
    private SkillTreeServerRegistry() {}

    // treeId -> (nodeId -> SkillTreeNode)
    private static final Map<Integer, Map<String, SkillTreeNode>> trees = new ConcurrentHashMap<>();
    private static boolean loaded = false;
    public static Set<Integer> getAllTreeIds() {
        return trees.keySet();
    }
    public static void load(MinecraftServer server) {
        if (loaded) return;
        loaded = true;
        trees.clear();

        try {
            ResourceManager rm = server.getResourceManager();
            String folder = "skill_tree";

            // Отладка: выведем ВСЕ ресурсы мода чтобы понять структуру путей
            System.out.println("[DamageCore] All damagecore resources:");
            var allResources = rm.listResources("", loc -> loc.getNamespace().equals("damagecore"));
            for (var loc : allResources.keySet()) {
                System.out.println("  -> " + loc);
            }

            var resources = rm.listResources(folder,
                    loc -> loc.getPath().startsWith(folder + "/") && loc.getPath().endsWith(".json"));

            System.out.println("[DamageCore] Found " + resources.size() + " resources in folder: " + folder);
            for (var loc : resources.keySet()) {
                System.out.println("  -> " + loc);
            }

            List<String> fileNames = new ArrayList<>();
            for (var loc : resources.keySet()) {
                fileNames.add(loc.getPath().substring(loc.getPath().lastIndexOf('/') + 1));
            }
            fileNames.sort(String.CASE_INSENSITIVE_ORDER);

            int tabId = 0;
            for (String fileName : fileNames) {
                String resourcePath = folder + "/" + fileName;

                Object[] result = SkillTreeLoader.loadFromResource(resourcePath, rm);
                if (result == null) continue;

                if (result[1] instanceof List<?> list && !list.isEmpty()) {
                    Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
                    for (Object o : list) {
                        if (o instanceof SkillTreeNode node) nodes.put(node.id, node);
                    }
                    trees.put(tabId, nodes);
                    tabId++;
                }
            }

            System.out.println("[DamageCore] Server loaded " + trees.size() + " skill trees.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reset() {
        trees.clear();
        loaded = false;
    }

    public static boolean hasTree(int treeId) {
        return trees.containsKey(treeId);
    }

    public static Map<String, SkillTreeNode> getNodes(int treeId) {
        return trees.getOrDefault(treeId, Collections.emptyMap());
    }

    public static SkillTreeNode getNode(int treeId, String nodeId) {
        Map<String, SkillTreeNode> nodes = trees.get(treeId);
        if (nodes == null) return null;
        return nodes.get(nodeId);
    }

    public static int getTotalTrees() {
        return trees.size();
    }
}