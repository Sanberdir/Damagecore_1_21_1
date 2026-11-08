package ru.imaginaerum.damagecore.api.skill_tree;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTreeLevels {
    private ClientTreeLevels() {}
    private static final Map<Integer, Integer> LEVELS = new ConcurrentHashMap<>();

    public static int get(int treeId) { return LEVELS.getOrDefault(treeId, 0); }
    public static void set(int treeId, int level) { LEVELS.put(treeId, level); }
    public static void clear() { LEVELS.clear(); }
}