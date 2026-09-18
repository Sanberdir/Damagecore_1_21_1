package ru.imaginaerum.damagecore.api.skill_tree.categories;

import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer;

import java.util.*;

public final class TreeCategoryManager {
    private TreeCategoryManager() {}

    public static final int CATEGORY_COUNT = 2;

    // category index -> список ИМЁН ФАЙЛОВ деревьев (как в /skill_tree/*.json), а не числовых id
    private static final Map<Integer, List<String>> CATEGORY_FILES = new HashMap<>();

    private static int activeCategory = 0;

    static {
        // ПОДСТАВЬТЕ СВОИ ИМЕНА ФАЙЛОВ (с расширением .json, как они лежат в resources/skill_tree/)
        CATEGORY_FILES.put(0, new ArrayList<>(List.of("alchemy.json", "shooting.json")));
        CATEGORY_FILES.put(1, new ArrayList<>(List.of("blocking.json")));
    }

    public static void setCategoryFiles(int category, List<String> fileNames) {
        CATEGORY_FILES.put(category, new ArrayList<>(fileNames));
    }

    public static List<String> getCategoryFiles(int category) {
        return CATEGORY_FILES.getOrDefault(category, Collections.emptyList());
    }

    public static int getActiveCategory() {
        return activeCategory;
    }

    /** @return true, если категория реально сменилась (клик по уже активной вкладке — no-op) */
    public static boolean setActiveCategory(int category) {
        if (category < 0 || category >= CATEGORY_COUNT) return false;
        if (activeCategory == category) return false;
        activeCategory = category;
        return true;
    }

    /**
     * Резолвит имена файлов активной категории в реальные global id (tabId),
     * которые сейчас присвоены в SkillTreeRenderer.fileNameToTabId.
     * Деревья, которых нет в загруженном наборе (файл не найден/не загрузился), пропускаются.
     */
    public static List<Integer> getVisibleTreeIds() {
        List<Integer> visible = new ArrayList<>();
        for (String fileName : getCategoryFiles(activeCategory)) {
            Integer tabId = SkillTreeRenderer.getTabIdForFileName(fileName);
            if (tabId != null && SkillTreeRenderer.hasTreeForTab(tabId)) {
                visible.add(tabId);
            }
        }
        return visible;
    }
}