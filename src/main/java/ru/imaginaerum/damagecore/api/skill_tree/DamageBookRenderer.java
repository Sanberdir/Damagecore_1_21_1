package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.imaginaerum.damagecore.api.skill_tree.categories.TreeCategoryManager;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.Render;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.SideTabsRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DamageBookRenderer {

    public static final int TAB_WIDTH = 150;

    private static final Map<Integer, Integer> TREE_XP = new HashMap<>();
    private static final Map<Integer, Integer> TREE_LEVEL = new HashMap<>();
    private static final int BASE_XP_PER_LEVEL = 3; // Можно настроить
    private static final double XP_GROWTH_FACTOR = 1.5; // Множитель роста
    public static Screen currentScreen = null;
    // ---- СХЕМА ВКЛАДОК ----
    public static final int MIDDLE_TABS = 8;
    public static final int SIDE_TABS = 2;
    public static final int TABS_PER_ROW = MIDDLE_TABS + SIDE_TABS;
    private static final int XP_BAR_U = 176;
    private static final int XP_BAR_V = 214;
    private static final int XP_BAR_WIDTH = 31;
    private static final int XP_BAR_HEIGHT = 5;
    public static final int ROWS = 1; // деревья теперь только внизу
    public static final int PAGE_SIZE = TABS_PER_ROW * ROWS;

    // ---- ВКЛАДКИ КАТЕГОРИЙ ----
    private static final int CATEGORY_TAB_W = 32;
    private static final int CATEGORY_TAB_H = 27;
    private static final int CATEGORY_TAB_GAP = 2;
    // Смещение по X относительно правого края панели.
    private static final int CATEGORY_TAB_X_OFFSET = -3;
    // Смещение по Y относительно верха панели.
    private static final int CATEGORY_TAB_Y_OFFSET = 6;

    public static int bottomLeft()  { return 0; }
    public static int bottomRight() { return TABS_PER_ROW - 1; }
    public static int bottomMiddle(int i) { return 1 + i; }

    public static int topLeft() { return TABS_PER_ROW; }
    public static int topRight() { return TABS_PER_ROW * 2 - 1; }
    public static int topMiddle(int i) { return TABS_PER_ROW + 1 + i; }

    // selectedBottomTab теперь хранит глобальный ID (index среди всех загруженных деревьев)
    // selectedBottomTab теперь хранит глобальный ID (index среди всех загруженных деревьев)
    public static int selectedBottomTab = 0;

    /** Сентинел: ни одно дерево не выбрано (например, открыта боковая вкладка брони/зелий). */
    public static final int NO_TREE_SELECTED = Integer.MIN_VALUE;
    public static void setBottomTab(int globalTabId) {
        selectedBottomTab = globalTabId;
    }

    /** Снимает выделение с нижнего ряда вкладок (деревья становятся "неактивными"). */
    public static void clearSelectedTree() {
        selectedBottomTab = NO_TREE_SELECTED;
    }
    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");

    private static void renderCategoryTabs(GuiGraphics gui, int panelLeft, int panelTop, int panelW, int mouseX, int mouseY) {
        final int TAB_W_IDLE = 30;
        final int TAB_H_IDLE = 28;
        final int TAB_U_IDLE = 208;
        final int TAB_V_IDLE = 179;

        final int TAB_W_ACTIVE = 32;
        final int TAB_H_ACTIVE = 27;
        final int TAB_U_ACTIVE = 240;
        final int TAB_V_ACTIVE = 208;

        final int GAP = 2;
        final int STEP = Math.max(TAB_H_IDLE, TAB_H_ACTIVE) + GAP;

        int x = panelLeft + panelW + CATEGORY_TAB_X_OFFSET;
        int yStart = panelTop + CATEGORY_TAB_Y_OFFSET;

        for (int cat = 0; cat < TreeCategoryManager.CATEGORY_COUNT; cat++) {
            int y = yStart + cat * STEP;
            boolean active = TreeCategoryManager.getActiveCategory() == cat;

            int u, v, w, h;
            if (active) {
                u = TAB_U_ACTIVE; v = TAB_V_ACTIVE;
                w = TAB_W_ACTIVE; h = TAB_H_ACTIVE;
            } else {
                u = TAB_U_IDLE;   v = TAB_V_IDLE;
                w = TAB_W_IDLE;   h = TAB_H_IDLE;
            }

            int drawX = active ? x + (TAB_W_IDLE - w) / 2 : x - 1;
            int drawY = y + (TAB_H_IDLE - h) / 2;

            gui.blit(DAMAGE_CORE_INTERFACE, drawX, drawY, u, v, w, h, 512, 512);

            // Иконка рута первого дерева категории
            ItemStack icon = ItemStack.EMPTY;
            for (String fileName : TreeCategoryManager.getCategoryFiles(cat)) {
                Integer tabId = SkillTreeRenderer.getTabIdForFileName(fileName);
                if (tabId != null && SkillTreeRenderer.hasTreeForTab(tabId)) {
                    icon = SkillTreeRenderer.getRootIcon(tabId);
                    if (icon != null && !icon.isEmpty()) break;
                }
            }

            if (!icon.isEmpty()) {
                int ix = drawX + (w - 16) / 2;
                int iy = drawY + (h - 16) / 2;
                gui.renderItem(icon, ix, iy);
            }
        }
    }

    /** true, если клик попал по вкладке категории (даже если это был no-op по активной). */
    public static boolean handleCategoryClick(double mouseX, double mouseY, int panelLeft, int panelTop, int panelW) {
        final int TAB_W = CATEGORY_TAB_W;
        final int TAB_H = CATEGORY_TAB_H;
        final int GAP   = CATEGORY_TAB_GAP;

        int x = panelLeft + panelW + CATEGORY_TAB_X_OFFSET; // синхронизировано с отрисовкой
        int yStart = panelTop + CATEGORY_TAB_Y_OFFSET;

        for (int cat = 0; cat < TreeCategoryManager.CATEGORY_COUNT; cat++) {
            int y = yStart + cat * (TAB_H + GAP);
            if (inside(mouseX, mouseY, x, y, TAB_W, TAB_H)) {
                switchCategory(cat);
                return true;
            }
        }
        return false;
    }

    // paging
    private static int currentPage = 0;

    private DamageBookRenderer() {}
    // Метод для расчета XP, необходимого для достижения следующего уровня
    public static int getXpRequiredForLevel(int level) {
        if (level < 0) return 0;
        return (int) Math.floor(BASE_XP_PER_LEVEL * Math.pow(XP_GROWTH_FACTOR, level));
    }

    public static int getXp(int treeId) {
        return TREE_XP.getOrDefault(treeId, 0);
    }

    public static int getLevel(int treeId) {
        return TREE_LEVEL.getOrDefault(treeId, 0);
    }

    public static void clearXpData() {
        TREE_XP.clear();
        TREE_LEVEL.clear();
    }

    public static void setXp(int treeId, int xp) {
        TREE_XP.put(treeId, xp);
    }

    public static void setLevel(int treeId, int level) {
        TREE_LEVEL.put(treeId, Math.min(level, 20));
    }

    public static void forceRefresh() {
        Minecraft.getInstance().tell(() -> {});
    }

    public static float getLevelProgress(int treeId) {
        int currentLevel = TREE_LEVEL.getOrDefault(treeId, 0);
        if (currentLevel >= 20) {
            return 1.0f;
        }

        int currentXp = TREE_XP.getOrDefault(treeId, 0);
        int xpForCurrentLevel = getXpRequiredForLevel(currentLevel);
        return (float) currentXp / xpForCurrentLevel;
    }

    private static void renderTabXp(
            GuiGraphics gui, int tabX,int tabY,int tabW,int tabH,boolean topTab,int treeId) {
        final int padding = 3;
        int availableWidth = tabW - padding * 2;
        if (availableWidth <= 0) return;

        float progress = getLevelProgress(treeId);
        int filledWidth = (int)(availableWidth * progress);

        int xpX = tabX + padding;
        int xpY = topTab ? (tabY + padding) : (tabY + tabH - XP_BAR_HEIGHT - padding);

        gui.blit(DAMAGE_CORE_INTERFACE,xpX,xpY,availableWidth,XP_BAR_HEIGHT,XP_BAR_U,XP_BAR_V, XP_BAR_WIDTH,XP_BAR_HEIGHT,512,512);

        if (filledWidth > 0) {
            int sourceWidth = (int)(XP_BAR_WIDTH * progress);
            if (sourceWidth < 1) sourceWidth = 1;

            gui.blit(DAMAGE_CORE_INTERFACE,xpX,xpY,filledWidth,XP_BAR_HEIGHT,XP_BAR_U,XP_BAR_V + XP_BAR_HEIGHT,sourceWidth,XP_BAR_HEIGHT,
                    512,512);
        }

        String levelText = String.valueOf(TREE_LEVEL.getOrDefault(treeId, 0));
        int textWidth = Minecraft.getInstance().font.width(levelText);
        int textX = tabX + (tabW - textWidth) / 2;
        int textY = topTab ? xpY - Minecraft.getInstance().font.lineHeight - 2 : xpY + XP_BAR_HEIGHT + 4;

        gui.drawString(Minecraft.getInstance().font, levelText, textX, textY, 0xFF00FF00, false);
    }

    public static Map<Integer, Integer> getTreeXp() {
        return new HashMap<>(TREE_XP);
    }

    public static Map<Integer, Integer> getTreeLevel() {
        return new HashMap<>(TREE_LEVEL);
    }

    // ---------- вычисление позиций ----------
    public static int calcMiddleGap(int panelLeft, int panelWidth, int tabW) {
        int startX = panelLeft + tabW;
        int endX = panelLeft + panelWidth - tabW;
        return (endX - startX - tabW * MIDDLE_TABS) / (MIDDLE_TABS - 1);
    }

    public static int calcMiddleX(int i, int panelLeft, int panelWidth, int tabW) {
        int startX = panelLeft + tabW;
        int gap = calcMiddleGap(panelLeft, panelWidth, tabW);
        return startX + i * (tabW + gap) + 1;
    }

    private static void drawTopUtilityTab(
            GuiGraphics gui,
            int x, int y,
            int tabW,
            int tabId,
            int activeSideTab,
            int u,
            int mouseX,
            int mouseY
    ) {
        boolean active = activeSideTab == tabId;

        int v = active ? 203 : 175;
        int h = active ? 32 : 27;
        int yOffset = active ? -3 : 1;

        gui.blit(DAMAGE_CORE_INTERFACE, x, y + yOffset, u, v, tabW, h, 512, 512);

        ItemStack icon = tabId == SideTabsRenderer.TAB_ARMOR
                ? new ItemStack(Items.DIAMOND_CHESTPLATE)
                : new ItemStack(Items.POTION);

        int ix = x + (tabW - 16) / 2;
        int iy = y + yOffset + (h - 16) / 2;
        gui.renderItem(icon, ix, iy);
    }

    // ---------- правая панель ----------
    public static void renderRightInterface(
            GuiGraphics gui,
            InventoryScreen screen,
            int x, int y,
            int mouseX,
            int mouseY,
            int activeSideTab
    ) {
        boolean sideTabActive = activeSideTab != SideTabsRenderer.TAB_NONE;

        int PANEL_W = 289;
        int PANEL_H = 166;
        int TAB_W = 28;

        int panelLeft = x + 2;
        int panelTop = y;

        gui.blit(DAMAGE_CORE_INTERFACE, panelLeft, panelTop, 179, 0, PANEL_W, PANEL_H, 512, 512);

        int TAB_Y = panelTop + 163;
        int TOP_Y = panelTop - 25;
        int TOP_TAB_GAP = 2;

        drawSideTab(gui, panelLeft, TAB_Y, TAB_W, globalIdForSlot(bottomLeft()), true, 0, 0, mouseX, mouseY);
        drawMiddleRow(gui, panelLeft, PANEL_W, TAB_Y, TAB_W, true, mouseX, mouseY);
        drawSideTab(gui, panelLeft + PANEL_W - TAB_W, TAB_Y, TAB_W, globalIdForSlot(bottomRight()), true, 56, 1, mouseX, mouseY);
        renderCategoryTabs(gui, panelLeft, panelTop, PANEL_W, mouseX, mouseY);

        drawTopUtilityTab(gui, panelLeft, TOP_Y, TAB_W,
                SideTabsRenderer.TAB_ARMOR, activeSideTab, 89, mouseX, mouseY);
        drawTopUtilityTab(gui, panelLeft + TAB_W + TOP_TAB_GAP, TOP_Y, TAB_W,
                SideTabsRenderer.TAB_POTION, activeSideTab, 116, mouseX, mouseY);

        int totalTrees = SkillTreeRenderer.getTotalTrees();
        int pageCount = Math.max(1, (totalTrees + PAGE_SIZE - 1) / PAGE_SIZE);

        if (totalTrees > PAGE_SIZE) {
            final int ARROW_U_RIGHT = 180;
            final int ARROW_U_LEFT = 194;
            final int ARROW_V = 176;
            final int ARROW_HOVER_V = 194;
            final int ARROW_W = 12;
            final int ARROW_H = 18;

            int leftArrowX = panelLeft - 2 - ARROW_W;
            int leftArrowY = TAB_Y + 3;
            int rightArrowX = panelLeft + PANEL_W + 2;
            int rightArrowY = TAB_Y + 3;

            boolean hoverLeft = inside(mouseX, mouseY, leftArrowX, leftArrowY, ARROW_W, ARROW_H);
            boolean hoverRight = inside(mouseX, mouseY, rightArrowX, rightArrowY, ARROW_W, ARROW_H);

            if (currentPage > 0) {
                int v = hoverLeft ? ARROW_HOVER_V : ARROW_V;
                gui.blit(DAMAGE_CORE_INTERFACE, leftArrowX, leftArrowY, ARROW_U_LEFT, v, ARROW_W, ARROW_H, 512, 512);
            }

            if (currentPage < pageCount - 1) {
                int v = hoverRight ? ARROW_HOVER_V : ARROW_V;
                gui.blit(DAMAGE_CORE_INTERFACE, rightArrowX, rightArrowY, ARROW_U_RIGHT, v, ARROW_W, ARROW_H, 512, 512);
            }

            String pageText = String.format("Page %d/%d", currentPage + 1, pageCount);
            int textX = panelLeft + (PANEL_W / 2) - (Minecraft.getInstance().font.width(pageText) / 2);
            int textY = panelTop + PANEL_H - 6;
            gui.drawString(Minecraft.getInstance().font, pageText, textX, textY, 0xFFCCCCCC, false);
        }

        if (!sideTabActive) {
            Render.render(gui, screen, panelLeft, panelTop, mouseX, mouseY);
        }
    }

    private static void drawMiddleRow(GuiGraphics gui, int panelLeft, int panelW, int y, int tabW, boolean bottom, int mouseX, int mouseY) {
        for (int i = 0; i < MIDDLE_TABS; i++) {
            int slotId = bottom ? bottomMiddle(i) : topMiddle(i);
            int globalId = globalIdForSlot(slotId);
            if (!SkillTreeRenderer.hasTreeForTab(globalId)) continue;

            int x = calcMiddleX(i, panelLeft, panelW, tabW);
            boolean active = selectedBottomTab == globalId;

            int v = active ? (bottom ? 204 : 203) : 175;
            int h = active ? 32 : 25;
            int u = bottom ? 28 : 117;

            int yOffset;

            if (active) {
                yOffset = bottom ? -1 : -3;
            } else {
                yOffset = bottom ? 2 : 1;
            }

            gui.blit(DAMAGE_CORE_INTERFACE, x, y + yOffset, u, v, tabW, h, 512, 512);

            if (active) {
                renderTabXp(gui, x, y + yOffset, tabW, h, !bottom, globalId);
            }
            drawRootIconCentered(gui, x, y + yOffset, tabW, h, globalId);
        }
    }

    private static void drawSideTab(
            GuiGraphics gui,
            int x, int y,
            int tabW,
            int globalId,
            boolean bottom,
            int u,
            int idleYOffset,
            int mouseX,
            int mouseY
    ) {
        if (!SkillTreeRenderer.hasTreeForTab(globalId)) return;

        boolean active = selectedBottomTab == globalId;

        int v = active
                ? (bottom ? 204 : 203)
                : (bottom ? 173 + idleYOffset : 175);

        int h = active ? 32 : 27;

        int yOffset = active
                ? (bottom ? -1 : -3)
                : idleYOffset;

        gui.blit(DAMAGE_CORE_INTERFACE, x, y + yOffset, u, v, tabW, h, 512, 512);

        if (active) {
            renderTabXp(gui, x, y + yOffset, tabW, h, !bottom, globalId);
        }
        drawRootIconCentered(gui, x, y + yOffset, tabW, h, globalId);
    }

    // ---------- paging API ----------

    public static int globalIdForSlot(int slotId) {
        List<Integer> visible = TreeCategoryManager.getVisibleTreeIds();
        int idx = currentPage * PAGE_SIZE + slotId;
        if (idx < 0 || idx >= visible.size()) return -1;
        return visible.get(idx);
    }

    public static void setCurrentPage(int page) {
        int total = TreeCategoryManager.getVisibleTreeIds().size();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        currentPage = page;
    }

    private static void selectFirstVisibleOnPage() {
        List<Integer> visible = TreeCategoryManager.getVisibleTreeIds();
        int start = currentPage * PAGE_SIZE;
        if (start < visible.size()) {
            int gid = visible.get(start);
            setBottomTab(gid);
            SkillTreeRenderer.setActiveTree(gid);
        }
    }

    public static int getCurrentPage() {
        return currentPage;
    }

    public static void nextPage() {
        setCurrentPage(currentPage + 1);
        selectFirstVisibleOnPage();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void switchCategory(int category) {
        if (TreeCategoryManager.setActiveCategory(category)) {
            currentPage = 0;

            // Если боковая вкладка активна — не выбираем дерево автоматически.
            // Дерево будет выбрано, только когда игрок сам кликнет по нижней вкладке.
            if (selectedBottomTab != NO_TREE_SELECTED) {
                selectFirstVisibleOnPage();
            }

            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public static void prevPage() {
        setCurrentPage(currentPage - 1);
        selectFirstVisibleOnPage();
    }

    public static boolean handleArrowClick(double mouseX, double mouseY, int panelLeft, int panelTop, int panelW) {
        int totalTrees = TreeCategoryManager.getVisibleTreeIds().size();
        if (totalTrees <= PAGE_SIZE) return false;

        final int ARROW_W = 12;
        final int ARROW_H = 18;

        int TAB_Y = panelTop + 163;
        int leftArrowX = panelLeft - 2 - ARROW_W;
        int leftArrowY = TAB_Y + 3;
        int rightArrowX = panelLeft + panelW + 2;
        int rightArrowY = TAB_Y + 3;

        int pageCount = Math.max(1, (totalTrees + PAGE_SIZE - 1) / PAGE_SIZE);

        if (inside(mouseX, mouseY, leftArrowX, leftArrowY, ARROW_W, ARROW_H)) {
            if (currentPage > 0) {
                prevPage();
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                return true;
            }
            return false;
        }

        if (inside(mouseX, mouseY, rightArrowX, rightArrowY, ARROW_W, ARROW_H)) {
            if (currentPage < pageCount - 1) {
                nextPage();
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                return true;
            }
            return false;
        }

        return false;
    }

    private static void drawRootIconCentered(
            GuiGraphics gui,
            int tabX,
            int tabY,
            int tabW,
            int tabH,
            int globalId
    ) {
        ItemStack icon = SkillTreeRenderer.getRootIcon(globalId);
        if (icon == null || icon.isEmpty()) return;

        int ix = tabX + (tabW - 16) / 2;
        int iy = tabY + (tabH - 16) / 2;

        gui.renderItem(icon, ix, iy);
    }
}