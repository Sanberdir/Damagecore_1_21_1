// EffectIconLayout.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

/** Общие размеры/отступы для всех значков на вкладке эффектов. */
public final class EffectIconLayout {
    private EffectIconLayout() {}

    public static final int ICON_SIZE = 16;
    public static final int GAP       = 2;
    public static final int BAR_HEIGHT = 2;
    public static final int BAR_GAP    = 1;
    /** Зазор между рядами (зелья/моб/еда) — больше GAP, т.к. gui.renderItem "вылезает" за рамку 16x16. */
    public static final int ROW_GAP    = 6;
}