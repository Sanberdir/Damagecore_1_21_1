package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.skill_tree.node_variant.SelectNodeVariantPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTreeRenderer {
    private SkillTreeRenderer() {}

    private static boolean treesLoaded = false;

    public static final int AREA_TEX_X0 = 187;
    public static final int AREA_TEX_Y0 = 8;
    public static final int AREA_TEX_X1 = 460;
    public static final int AREA_Y1 = 158;
    public static final int AREA_WIDTH = AREA_TEX_X1 - AREA_TEX_X0;
    public static final int AREA_HEIGHT = AREA_Y1 - AREA_TEX_Y0;

    private static final int PANEL_TEXTURE_U = 179;
    private static final int PANEL_TEXTURE_V = 0;
    public static final int PANEL_DRAW_OFFSET_X_IN_PANEL = AREA_TEX_X0 - PANEL_TEXTURE_U;
    public static final int PANEL_DRAW_OFFSET_Y_IN_PANEL = AREA_TEX_Y0 - PANEL_TEXTURE_V;

    private static final int GRID_STEP = SkillTreeNode.FRAME_SIZE + 1;
    private static final int OPTION_SIZE = 18;
    private static final int OPTION_BASE_RADIUS = 28;
    private static final int OPTION_RADIUS_STEP = 8;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

    private static long lastClickTime = 0;
    private static final long DOUBLE_CLICK_MS = 300;

    private static final Map<Integer, SkillTreeData> trees = new ConcurrentHashMap<>();
    private static final Map<String, Integer> fileNameToTabId = new HashMap<>();
    private static List<String> sortedFileNames = new ArrayList<>();
    private static int activeTreeId = 0;

    /** Внутреннее представление дерева */
    private static class SkillTreeData {
        final Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
        final Map<String, List<SkillTreeNode>> childrenMap = new HashMap<>();

        boolean isDragging = false;
        int dragStartX = 0, dragStartY = 0;
        int dragStartOffsetX = 0, dragStartOffsetY = 0;

        int offsetX = 0, offsetY = 0;
        float scale = 1.0f;

        String fileName;
        String displayName;
        ItemStack tabIcon = ItemStack.EMPTY;

        String activeOptionsNodeId = null;

        SkillTreeData(String fileName, String displayName) {
            this.fileName = fileName;
            this.displayName = displayName;
        }
    }
    public static void clearAllCaches() {

        // 1) Сначала сбросим состояния нод (level, selectedOption) чтобы UI точно не показывал старые значения
        for (SkillTreeData tree : trees.values()) {
            for (SkillTreeNode node : tree.nodes.values()) {
                node.resetNode(); // должен обнулять level и selectedOption
            }
            tree.activeOptionsNodeId = null;
            tree.offsetX = 0;
            tree.offsetY = 0;
            tree.scale = 1.0f;
        }

        // 2) Если есть клиентский кэш - почистим его (SkillTreeClientSync реализуй ниже)
        try {
            SkillTreeClientSync.clearCache();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // 3) Теперь окончательно удалим структуры (чтобы loadAllTrees мог перезагрузить их)
        trees.clear();
        fileNameToTabId.clear();
        sortedFileNames.clear();

        activeTreeId = 0;
        treesLoaded = false;
    }

    public static void resetAllNodes() {
        for (var treeEntry : trees.entrySet()) {
            var tree = treeEntry.getValue();
            for (var node : tree.nodes.values()) {
                node.resetNode(); // сброс уровня, выбранного варианта
            }
        }
    }
    // --- Работа с активным деревом ---
    private static SkillTreeData getCurrentTree() {
        return trees.computeIfAbsent(activeTreeId,
                id -> new SkillTreeData("empty", "Empty Tree " + (activeTreeId + 1)));
    }
    public static void clearLearnedData() {
        for (SkillTreeData tree : trees.values()) {
            for (SkillTreeNode node : tree.nodes.values()) {
                node.resetNode(); // сбросить уровень, изученные варианты, флаги изучения
            }
            tree.activeOptionsNodeId = null;
        }
    }
    public static void setActiveTree(int tabId) {
        if (trees.containsKey(tabId)) activeTreeId = tabId;
    }

    // --- Опции нод ---
    private static boolean openOptionsForNode(SkillTreeData tree, SkillTreeNode node) {
        if (node == null || node.options == null || node.options.isEmpty() || node.isLearned()) return false;
        tree.activeOptionsNodeId = node.id;
        return true;
    }

    private static void closeOptions(SkillTreeData tree) {
        tree.activeOptionsNodeId = null;
    }

    private static int optionIndexAtPoint(SkillTreeNode node, int px, int py, int optionSize) {
        int n = node.options.size();
        if (n == 0) return -1;

        int radius = OPTION_BASE_RADIUS + Math.max(0, n - 1) * OPTION_RADIUS_STEP;
        for (int i = 0; i < n; i++) {
            double angle = 2.0 * Math.PI * i / n;
            int cx = node.centerX() + (int)Math.round(radius * Math.cos(angle));
            int cy = node.centerY() + (int)Math.round(radius * Math.sin(angle));

            int left = cx - optionSize / 2;
            int top = cy - optionSize / 2;
            if (px >= left && px < left + optionSize && py >= top && py < top + optionSize) return i;
        }
        return -1;
    }

    // --- Загрузка деревьев ---
    public static void loadAllTrees(String folderPath) {
        if (treesLoaded) return;
        treesLoaded = true;

        trees.clear();
        fileNameToTabId.clear();
        sortedFileNames.clear();

        try {
            String path = folderPath.replaceAll("^/|/$", "");
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resources = resourceManager.listResources(path, loc -> loc.getPath().startsWith(path + "/") && loc.getPath().endsWith(".json"));

            for (var loc : resources.keySet()) {
                String fileName = loc.getPath().substring(loc.getPath().lastIndexOf('/') + 1);
                sortedFileNames.add(fileName);
            }
            sortedFileNames.sort(String.CASE_INSENSITIVE_ORDER);

            int tabId = 0;
            for (String fileName : sortedFileNames) {
                String resourcePath = path + "/" + fileName;
                String displayName = fileName.replace(".json", "");
                Object[] loaderResult = SkillTreeLoader.loadFromResource(resourcePath);

                ItemStack tabIcon = ItemStack.EMPTY;
                List<SkillTreeNode> nodesList = Collections.emptyList();
                if (loaderResult != null) {
                    if (loaderResult[0] instanceof ItemStack) tabIcon = (ItemStack) loaderResult[0];
                    if (loaderResult[1] instanceof List<?> list) nodesList = (List<SkillTreeNode>) list;
                }
                System.out.println("[Trees] tabId=" + tabId + " file=" + fileName);

                if (!nodesList.isEmpty()) {
                    SkillTreeData tree = new SkillTreeData(fileName, displayName);
                    for (SkillTreeNode n : nodesList) tree.nodes.put(n.id, n);
                    rebuildChildrenMap(tree);
                    tree.tabIcon = tabIcon;
                    trees.put(tabId, tree);
                    fileNameToTabId.put(fileName, tabId);
                    tabId++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Применяем кэш уровней после загрузки
        SkillTreeClientSync.reapplyCachedForAllTrees();
    }

    private static void rebuildChildrenMap(SkillTreeData tree) {
        tree.childrenMap.clear();
        for (SkillTreeNode n : tree.nodes.values()) {
            for (String pid : n.parentIds) {
                if (pid != null && !"start".equalsIgnoreCase(pid)) {
                    tree.childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(n);
                }
            }
        }
    }

    // --- Позиционирование нод ---
    private static void calculateAndUpdatePositions(SkillTreeData tree, int panelScreenX, int panelScreenY) {
        if (tree.nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
        int centerX = areaX + AREA_WIDTH / 2 + tree.offsetX;
        int centerY = areaY + AREA_HEIGHT / 2 + tree.offsetY;

        List<SkillTreeNode> roots = tree.nodes.values().stream().filter(SkillTreeNode::isRoot).toList();
        if (roots.isEmpty() && !tree.nodes.isEmpty()) roots.add(tree.nodes.values().iterator().next());

        Set<Long> occupied = new HashSet<>();
        var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>) (x,y) -> (((long)x)<<32)|(y&0xffffffffL);

        int rootX = 0;
        for (SkillTreeNode root : roots) {
            if (!root.hasGridPos) root.setGridPos(rootX,0);
            occupied.add(keyOf.apply(root.gridX, root.gridY));
            rootX += 2;
        }

        for (SkillTreeNode n : tree.nodes.values()) if (n.hasGridPos) occupied.add(keyOf.apply(n.gridX, n.gridY));

        Queue<SkillTreeNode> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            SkillTreeNode parent = queue.poll();
            for (SkillTreeNode child : tree.childrenMap.getOrDefault(parent.id, Collections.emptyList())) {
                if (child.hasGridPos) { queue.add(child); continue; }
                if (child.parentIds.size() > 1) positionMultiParentNode(child, tree, occupied);
                else positionSingleParentNode(child, parent, occupied);
                queue.add(child);
            }
        }

        for (SkillTreeNode n : tree.nodes.values()) {
            n.x = centerX + n.gridX*GRID_STEP - SkillTreeNode.FRAME_SIZE/2;
            n.y = centerY - n.gridY*GRID_STEP - SkillTreeNode.FRAME_SIZE/2;
        }
    }

    private static void positionSingleParentNode(SkillTreeNode child, SkillTreeNode parent, Set<Long> occupied) {
        int gx = parent.gridX;
        int gy = parent.gridY;
        switch(child.side) {
            case RIGHT -> gx++; case LEFT -> gx--; case TOP -> gy--; case BOTTOM -> gy++; default -> gx++;
        }
        var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>) (x,y) -> (((long)x)<<32)|(y&0xffffffffL);
        while (occupied.contains(keyOf.apply(gx, gy))) gx++;
        child.setGridPos(gx, gy);
        occupied.add(keyOf.apply(gx, gy));
    }

    private static void positionMultiParentNode(SkillTreeNode child, SkillTreeData tree, Set<Long> occupied) {
        // исправлено здесь: SkillTreeNode::hasGridPos -> n -> n.hasGridPos
        List<SkillTreeNode> parents = child.parentIds.stream()
                .map(tree.nodes::get)
                .filter(Objects::nonNull)
                .filter(n -> n.hasGridPos) // <-- исправлено
                .toList();

        if (parents.isEmpty()) {
            positionSingleParentNode(child, tree.nodes.values().iterator().next(), occupied);
            return;
        }

        int avgX = parents.stream().mapToInt(p -> p.gridX).sum() / parents.size();
        int avgY = parents.stream().mapToInt(p -> p.gridY).sum() / parents.size();

        switch (child.side) {
            case RIGHT -> avgX++;
            case LEFT -> avgX--;
            case TOP -> avgY--;
            case BOTTOM -> avgY++;
            default -> avgX++;
        }

        var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>) (x,y) -> (((long)x)<<32)|(y&0xffffffffL);
        int radius = 0;
        while (true) {
            for (int dx = -radius; dx <= radius; dx++)
                for (int dy = -radius; dy <= radius; dy++)
                    if (Math.abs(dx) == radius || Math.abs(dy) == radius) {
                        int tx = avgX + dx, ty = avgY + dy;
                        if (!occupied.contains(keyOf.apply(tx, ty))) {
                            child.setGridPos(tx, ty);
                            occupied.add(keyOf.apply(tx, ty));
                            return;
                        }
                    }
            radius++;
        }
    }

    // --- Взаимодействие с мышью ---

    public static boolean mousePressed(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        SkillTreeData tree = getCurrentTree();
        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        if (!(mouseX >= areaX && mouseX <= areaX + AREA_WIDTH && mouseY >= areaY && mouseY <= areaY + AREA_HEIGHT)) {
            if (tree.activeOptionsNodeId != null) closeOptions(tree);
            return tree.activeOptionsNodeId != null;
        }

        int pivotX = areaX + AREA_WIDTH / 2;
        int pivotY = areaY + AREA_HEIGHT / 2;
        int unscaledX = (int) ((mouseX - pivotX) / tree.scale + pivotX);
        int unscaledY = (int) ((mouseY - pivotY) / tree.scale + pivotY);

        if (tree.activeOptionsNodeId != null) {
            SkillTreeNode node = tree.nodes.get(tree.activeOptionsNodeId);
            if (node != null) {
                int idx = optionIndexAtPoint(node, unscaledX, unscaledY, OPTION_SIZE);
                if (idx >= 0) {
                    node.applyVariant(idx); // применяем сразу на клиенте

                    // ИСПРАВЛЕНО: Новый сетевой синтаксис отправки пакетов NeoForge 1.21.1
                    try {
                        PacketDistributor.sendToServer(new SelectNodeVariantPacket(
                                activeTreeId,
                                node.id,
                                idx
                        ));
                    } catch (Throwable ignored) {}
                }
            }
            closeOptions(tree);
            return true;
        }

        for (SkillTreeNode node : tree.nodes.values()) {
            if (node.containsPoint(unscaledX, unscaledY)) {
                if (button == 0) {
                    tree.isDragging = true;
                    tree.dragStartX = mouseX;
                    tree.dragStartY = mouseY;
                    tree.dragStartOffsetX = tree.offsetX;
                    tree.dragStartOffsetY = tree.offsetY;
                }

                // Двойной клик для открытия/закрытия опций
                if (!node.isLearned() && node.options != null && !node.options.isEmpty()) {
                    long now = System.currentTimeMillis();
                    if (now - lastClickTime < DOUBLE_CLICK_MS) {
                        if (tree.activeOptionsNodeId != null && tree.activeOptionsNodeId.equals(node.id)) {
                            closeOptions(tree);
                        } else {
                            openOptionsForNode(tree, node);
                        }
                    }
                    lastClickTime = now;
                }
                return true;
            }
        }

        if (button == 0) {
            tree.isDragging = true;
            tree.dragStartX = mouseX;
            tree.dragStartY = mouseY;
            tree.dragStartOffsetX = tree.offsetX;
            tree.dragStartOffsetY = tree.offsetY;
            return true;
        }
        return false;
    }


    public static boolean mouseDragged(int mouseX,int mouseY,int button,int panelScreenX,int panelScreenY) {
        SkillTreeData tree = getCurrentTree();
        if (!tree.isDragging || button!=0) return false;
        int deltaX = (int)((mouseX-tree.dragStartX)/tree.scale);
        int deltaY = (int)((mouseY-tree.dragStartY)/tree.scale);
        tree.offsetX = tree.dragStartOffsetX + deltaX;
        tree.offsetY = tree.dragStartOffsetY + deltaY;
        calculateAndUpdatePositions(tree,panelScreenX,panelScreenY);
        return true;
    }

    public static boolean mouseReleased(int mouseX,int mouseY,int button) {
        SkillTreeData tree = getCurrentTree();
        if (tree.isDragging) { tree.isDragging=false; return true; }
        return false;
    }

    public static boolean mouseScrolled(int mouseX,int mouseY,double delta,int panelScreenX,int panelScreenY) {
        SkillTreeData tree = getCurrentTree();
        tree.scale += (delta>0?ZOOM_STEP:-ZOOM_STEP);
        tree.scale=Math.max(MIN_SCALE,Math.min(MAX_SCALE,tree.scale));
        calculateAndUpdatePositions(tree,panelScreenX,panelScreenY);
        return true;
    }

    // --- Получение информации ---
    public static boolean hasTreeForTab(int tabId) { return trees.containsKey(tabId); }
    public static void resetTreePosition() { SkillTreeData tree = getCurrentTree(); tree.offsetX=0; tree.offsetY=0; tree.scale=1.0f; }
    public static ItemStack getRootIcon(int treeId) {
        SkillTreeData data = trees.get(treeId);
        if (data==null) return ItemStack.EMPTY;
        if (data.tabIcon!=null && !data.tabIcon.isEmpty()) return data.tabIcon;
        for (SkillTreeNode n : data.nodes.values()) if (n.isRoot() || "root".equalsIgnoreCase(n.id)) return n.itemStack;
        return ItemStack.EMPTY;
    }
    public static boolean isEmpty() { SkillTreeData tree = trees.get(activeTreeId); return tree==null || tree.nodes.isEmpty(); }
    public static float getScale() { return getCurrentTree().scale; }
    public static int getOffsetX() { return getCurrentTree().offsetX; }
    public static int getOffsetY() { return getCurrentTree().offsetY; }
    public static int getTotalTrees() { return trees.size(); }
    public static List<String> getSortedFileNames() { return new ArrayList<>(sortedFileNames); }
}