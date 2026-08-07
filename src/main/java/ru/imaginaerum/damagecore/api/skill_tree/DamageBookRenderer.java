package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.Render;

import java.util.HashMap;
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
    public static final int ROWS = 2;
    public static final int PAGE_SIZE = TABS_PER_ROW * ROWS;
    private static final int XP_BAR_U = 176;
    private static final int XP_BAR_V = 214;
    private static final int XP_BAR_WIDTH = 31;
    private static final int XP_BAR_HEIGHT = 5;
    public static int bottomLeft()  { return 0; }
    public static int bottomRight() { return TABS_PER_ROW - 1; }
    public static int bottomMiddle(int i) { return 1 + i; }

    public static int topLeft() { return TABS_PER_ROW; }
    public static int topRight() { return TABS_PER_ROW * 2 - 1; }
    public static int topMiddle(int i) { return TABS_PER_ROW + 1 + i; }

    // selectedBottomTab теперь хранит глобальный ID (index среди всех загруженных деревьев)
    public static int selectedBottomTab = 0;

    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");


    // paging
    private static int currentPage = 0;

    private DamageBookRenderer() {}
    // Метод для расчета XP, необходимого для достижения следующего уровня
    public static int getXpRequiredForLevel(int level) {
        if (level < 0) return 0;
        // Формула: базовый XP * (growthFactor ^ level)
        // Для level=0 (1-й уровень) возвращаем BASE_XP_PER_LEVEL
        return (int) Math.floor(BASE_XP_PER_LEVEL * Math.pow(XP_GROWTH_FACTOR, level));
    }
    // 2. ПОТОМ новые методы-геттеры/сеттеры
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
    public static void renderRightInterfaceNoTabs(
            GuiGraphics gui,
            InventoryScreen screen,
            int x, int y,
            int mouseX,
            int mouseY
    ) {
        int PANEL_W = 289;
        int PANEL_H = 166;

        int panelLeft = x + 2;
        int panelTop  = y;

        // Только фон панели, без вкладок и без дерева
        gui.blit(DAMAGE_CORE_INTERFACE, panelLeft, panelTop, 179, 0, PANEL_W, PANEL_H, 512, 512);
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
    // Метод для расчета текущего прогресса (0.0 - 1.0)
    public static float getLevelProgress(int treeId) {
        int currentLevel = TREE_LEVEL.getOrDefault(treeId, 0);
        if (currentLevel >= 20) {
            return 1.0f; // полоска полностью заполнена на уровне 20
        }

        int currentXp = TREE_XP.getOrDefault(treeId, 0);
        int xpForCurrentLevel = getXpRequiredForLevel(currentLevel);
        return (float) currentXp / xpForCurrentLevel;
    }


    // Обновляем renderTabXp для использования нового прогресса
    private static void renderTabXp(
            GuiGraphics gui, int tabX,int tabY,int tabW,int tabH,boolean topTab,int treeId) {
        final int padding = 3;
        int availableWidth = tabW - padding * 2;
        if (availableWidth <= 0) return;

        float progress = getLevelProgress(treeId);
        int filledWidth = (int)(availableWidth * progress);

        int xpX = tabX + padding;
        int xpY = topTab ? (tabY + padding) : (tabY + tabH - XP_BAR_HEIGHT - padding);

        // Рисуем фон
        gui.blit(DAMAGE_CORE_INTERFACE,xpX,xpY,availableWidth,XP_BAR_HEIGHT,XP_BAR_U,XP_BAR_V, XP_BAR_WIDTH,XP_BAR_HEIGHT,512,512);

        // Рисуем заполнение прогресса
        if (filledWidth > 0) {
            int sourceWidth = (int)(XP_BAR_WIDTH * progress);
            if (sourceWidth < 1) sourceWidth = 1;

            gui.blit(DAMAGE_CORE_INTERFACE,xpX,xpY,filledWidth,XP_BAR_HEIGHT,XP_BAR_U,XP_BAR_V + XP_BAR_HEIGHT,sourceWidth,XP_BAR_HEIGHT,
                    512,512);
        }

        // Рисуем уровень
        String levelText = String.valueOf(TREE_LEVEL.getOrDefault(treeId, 0));
        int textWidth = Minecraft.getInstance().font.width(levelText);
        int textX = tabX + (tabW - textWidth) / 2;
        int textY = topTab ? xpY - Minecraft.getInstance().font.lineHeight - 2 : xpY + XP_BAR_HEIGHT + 4;

        gui.drawString(Minecraft.getInstance().font, levelText, textX, textY, 0xFF00FF00, false);

        // Опционально: рисуем точное значение XP при наведении
        // Это можно добавить позже в обработчик тултипов
    }
    // Добавить в класс DamageBookRenderer:


    // Для получения данных для сохранения
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



    // ---------- правая панель ----------
    public static void renderRightInterface(
            GuiGraphics gui,
            InventoryScreen screen,
            int x, int y,
            int mouseX,
            int mouseY,
            boolean sideTabActive
    ) {
        int PANEL_W = 289;
        int PANEL_H = 166;
        int TAB_W = 28;

        int panelLeft = x + 2;
        int panelTop = y;

        gui.blit(DAMAGE_CORE_INTERFACE, panelLeft, panelTop, 179, 0, PANEL_W, PANEL_H, 512, 512);

        int TAB_Y = panelTop + 163;
        int TOP_Y = panelTop - 25;

        drawSideTab(gui, panelLeft, TAB_Y, TAB_W, globalIdForSlot(bottomLeft()), true, 0, 0, mouseX, mouseY);
        drawMiddleRow(gui, panelLeft, PANEL_W, TAB_Y, TAB_W, true, mouseX, mouseY);
        drawSideTab(gui, panelLeft + PANEL_W - TAB_W, TAB_Y, TAB_W, globalIdForSlot(bottomRight()), true, 56, 1, mouseX, mouseY);

        drawSideTab(gui, panelLeft, TOP_Y, TAB_W, globalIdForSlot(topLeft()), false, 89, 1, mouseX, mouseY);
        drawMiddleRow(gui, panelLeft, PANEL_W, TOP_Y, TAB_W, false, mouseX, mouseY);
        drawSideTab(gui, panelLeft + PANEL_W - TAB_W, TOP_Y, TAB_W, globalIdForSlot(topRight()), false, 145, 1, mouseX, mouseY);

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
            // пример внутри drawSideTab / drawMiddleRow

            if (active) {
                // полоска и цифра только для активной вкладки
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
            // полоска и цифра только для активной вкладки
            renderTabXp(gui, x, y + yOffset, tabW, h, !bottom, globalId);
        }
        // ⭐ вот этого не хватало
        drawRootIconCentered(gui, x, y + yOffset, tabW, h, globalId);
    }


    public static void setBottomTab(int globalTabId) {
        selectedBottomTab = globalTabId;
    }

    // ---------- paging API ----------

    public static int globalIdForSlot(int slotId) {
        return currentPage * PAGE_SIZE + slotId;
    }

    public static int getCurrentPage() {
        return currentPage;
    }

    public static void setCurrentPage(int page) {
        int total = SkillTreeRenderer.getTotalTrees();
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        currentPage = page;
    }

    public static void nextPage() {
        setCurrentPage(currentPage + 1);
        // при смене страницы, если текущий selectedBottomTab вне видимой страницы — переключаем на первый доступный там
        selectFirstVisibleOnPage();
    }

    public static void prevPage() {
        setCurrentPage(currentPage - 1);
        selectFirstVisibleOnPage();
    }

    private static void selectFirstVisibleOnPage() {
        // Найдём первый существующий глобальный ID на странице и выберем его
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int gid = currentPage * PAGE_SIZE + slot;
            if (SkillTreeRenderer.hasTreeForTab(gid)) {
                setBottomTab(gid);
                SkillTreeRenderer.setActiveTree(gid);
                return;
            }
        }
        // если там нет деревьев (маловероятно), оставим как было
    }

    // проверка попадания мыши в прямоугольник
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // Обработчик клика по стрелкам — вызывается извне (InventoryScreenMixin) при клике
    public static boolean handleArrowClick(double mouseX, double mouseY, int panelLeft, int panelTop, int panelW) {
        int totalTrees = SkillTreeRenderer.getTotalTrees();
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
