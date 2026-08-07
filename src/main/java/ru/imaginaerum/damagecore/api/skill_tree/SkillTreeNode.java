package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Node с поддержкой уровней (stackable node), вариантами и мультиродителями.
 *
 * Замечание: поля level/maxLevel сделаны public для совместимости с существующим кодом,
 * но также предоставлены геттеры/сеттеры и утилиты.
 */
public final class SkillTreeNode {
    public enum Side { START, LEFT, RIGHT, TOP, BOTTOM }
    public boolean blockedByTreeLevel = false; // true если не хватает уровня вкладки

    public boolean optionsVisible = false;
    public final String id;
    public String displayId;
    public ItemStack itemStack;
    public boolean locked;

    // уровень текущий и максимум уровней для этой ноды (stackable node)
    // сделаны public чтобы старый код (который обращается напрямую) компилировался
    public int level = 0;
    public int maxLevel = 1;
    // Добавление требований к уровню
    // требование по уровню вкладки (0 = нет требования)
    public int requiredTreeLevel = 0;

    public int getRequiredTreeLevel() { return requiredTreeLevel; }
    public void setRequiredTreeLevel(int lvl) { this.requiredTreeLevel = Math.max(0, lvl); }
    // Изменяем с одного родителя на список
    public final List<String> parentIds;
    public final Side side;

    public long xpFailFlashUntil = 0L;

    public int x;
    public int y;

    public boolean hasGridPos = false;
    public int gridX = 0;
    public int gridY = 0;

    public static final int FRAME_SIZE = 24;
    public static final int FRAME_PADDING = 2;

    public final List<ItemStack> options = new ArrayList<>();
    public int selectedOption = -1;

    // Конструктор для обратной совместимости (один родитель)
    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, String parentId, Side side) {
        this(id, itemStack, locked,
                parentId != null ? new ArrayList<>(Collections.singletonList(parentId)) : new ArrayList<>(),
                side);
    }
    public boolean isUnlocked(Map<String, Integer> levels, int playerTreeLevel) {
        // 1. Проверка уровня вкладки
        if (this.requiredTreeLevel > playerTreeLevel) {
            return false;
        }

        // 2. Корневая нода всегда доступна
        if (isRoot()) {
            return true;
        }

        // 3. Проверка родителей
        for (String parentId : parentIds) {
            if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;

            int parentLevel = levels.getOrDefault(parentId, 0);
            if (parentLevel <= 0) {
                return false;
            }
        }

        return true;
    }
    // Новый конструктор для нескольких родителей
    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, List<String> parentIds, Side side) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.displayId = this.id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentIds = parentIds != null ? new ArrayList<>(parentIds) : new ArrayList<>();
        this.side = side;
        this.x = 0;
        this.y = 0;
        this.hasGridPos = false;
        this.gridX = 0;
        this.gridY = 0;
        this.level = 0;
        this.maxLevel = 1;
    }

    public boolean isRoot() {
        return parentIds.isEmpty() || (parentIds.size() == 1 && "start".equalsIgnoreCase(parentIds.get(0)));
    }
    public void resetNode() {
        this.level = 0;
        this.selectedOption = -1;
        // не трогаем locked, grid-pos и прочие статичные конфигурации
    }
    /** Вспомогательные геттеры/сеттеры */
    public int getLevel() { return level; }
    public void setLevel(int lvl) { this.level = Math.max(0, Math.min(lvl, maxLevel)); }

    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int ml) { this.maxLevel = Math.max(1, ml); if (this.level > this.maxLevel) this.level = this.maxLevel; }

    public boolean isLearned() { return level > 0; }
    public boolean isMaxLevel() { return level >= maxLevel; }

    public boolean canLevelUp(Set<String> parentsThatAreAtLeastOne) {
        if (isMaxLevel()) return false;
        if (isRoot()) return true;

        for (String parentId : parentIds) {
            if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;
            if (!parentsThatAreAtLeastOne.contains(parentId)) {
                return false; // хотя бы один родитель не достиг уровня ≥1
            }
        }
        return true;
    }

    public void levelUp() {
        if (level < maxLevel) level++;
    }

    public void setGridPos(int gx, int gy) {
        this.hasGridPos = true;
        this.gridX = gx;
        this.gridY = gy;
    }

    public static class Variant {
        public final String displayId;
        public final ItemStack stack;

        public Variant(String displayId, ItemStack stack) {
            this.displayId = displayId;
            this.stack = stack;
        }

        public ItemStack getItemStack() { return stack; }
        public String getDisplayId() { return displayId; }
    }

    public final List<Variant> variants = new ArrayList<>();

    public void applyVariant(int index) {
        if (variants == null || index < 0 || index >= variants.size()) return;
        Variant v = variants.get(index);
        if (v == null) return;

        this.itemStack = v.stack;
        this.displayId = v.displayId;
        this.selectedOption = index;
    }

    public int centerX() { return x + FRAME_SIZE / 2; }
    public int centerY() { return y + FRAME_SIZE / 2; }

    public boolean containsPoint(int px, int py) {
        return px >= x && px < x + FRAME_SIZE && py >= y && py < y + FRAME_SIZE;
    }
}