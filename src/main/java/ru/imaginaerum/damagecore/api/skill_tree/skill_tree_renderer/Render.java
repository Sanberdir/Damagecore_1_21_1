package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.skill_tree.DamageBookRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.LearnNodePacket;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeClientSync;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Render {
    // copy of necessary constants (must match SkillTreeRenderer)
    public static final int AREA_TEX_X0 = 187;
    public static final int AREA_TEX_Y0 = 8;
    public static final int AREA_TEX_X1 = 460;
    public static final int AREA_TEX_Y1 = 158;
    public static final int AREA_WIDTH = AREA_TEX_X1 - AREA_TEX_X0;
    public static final int AREA_HEIGHT = AREA_TEX_Y1 - AREA_TEX_Y0;

    private static final int PANEL_TEXTURE_U = 179;
    private static final int PANEL_TEXTURE_V = 0;
    public static final int PANEL_DRAW_OFFSET_X_IN_PANEL = AREA_TEX_X0 - PANEL_TEXTURE_U; // 8
    public static final int PANEL_DRAW_OFFSET_Y_IN_PANEL = AREA_TEX_Y0 - PANEL_TEXTURE_V; // 8

    public static int currentPanelScreenX = 0;
    public static int currentPanelScreenY = 0;
    public static SkillTreeNode currentHoveredNode = null;
    public static long mousePressTime = 0L;
    private static final long FAIL_FLASH_DURATION = 250L;


    private static final int LINE_COLOR_LOCKED = 0xFF222222; // тёмно-серый
    private static final int LINE_COLOR_LEARNABLE = 0xFFDDDDDD; // светло-серый / белый
    private static final int LINE_COLOR_LEARNED = 0xFF95E674; // светло-зелёный (салатовый)
    public static void triggerXpFailFlash(SkillTreeNode node) {
        node.xpFailFlashUntil = System.currentTimeMillis() + FAIL_FLASH_DURATION;
    }
    private static void renderXpFailFlash(GuiGraphics gui, SkillTreeNode node) {
        if (node == null) return;
        try {
            Object treeObj = invokePrivateGetCurrentTree();
            if (treeObj == null) return;
            System.out.println("[RenderDebug] treeObj=" + treeObj.getClass()
                    + " nodesSize=" + ((Map<?,?>) getFieldValue(treeObj, "nodes")).size());
            // проверяем таймер
            long now = System.currentTimeMillis();
            if (node.xpFailFlashUntil <= now) return;

            // альфа и цвет (как было)
            float alpha = (node.xpFailFlashUntil - now) / (float) FAIL_FLASH_DURATION;
            alpha = Math.max(0f, Math.min(1f, alpha));
            int alphaInt = (int)(alpha * 136f) & 0xFF;
            int color = (alphaInt << 24) | 0x00FF0000;

            // scale и pivot — берём точно так же, как в renderHoldProgressOverlay
            float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();
            int clipX1 = currentPanelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
            int clipY1 = currentPanelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
            int pivotX = clipX1 + AREA_WIDTH / 2;
            int pivotY = clipY1 + AREA_HEIGHT / 2;

            PoseStack pose = gui.pose();
            pose.pushPose();

            // Применяем тот же pivot + scale, как у зелёного прогресса
            pose.translate(pivotX, pivotY, 2000);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            int frameSize = SkillTreeNode.FRAME_SIZE;
            int padding = SkillTreeNode.FRAME_PADDING;

            int left = node.x + padding;
            int top = node.y + padding;
            int width = frameSize - 2 * padding;
            int height = frameSize - 2 * padding;

            // Красная заливка полностью (в отличие от зелёного прогресса)
            gui.fill(left, top, left + width, top + height, color);

            pose.popPose();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /**
     * Рисует полупрозрачный зелёный прогресс удержания над нодой.
     * ВЫЗЫВАЙ после того, как нарисовал(а) ноды (чтобы заливка была поверх).
     * Использует GuiGraphics.fill(...) — корректно для Minecraft 1.20.1 (Forge).
     */

    public static SkillTreeNode getHoveredNodeUnderMouse(int mouseX, int mouseY) {
        Object treeObj = invokePrivateGetCurrentTree();
        if (treeObj == null) return null;

        int clipX1 = currentPanelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int clipY1 = currentPanelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
        int clipX2 = clipX1 + AREA_WIDTH;
        int clipY2 = clipY1 + AREA_HEIGHT;

        if (mouseX < clipX1 || mouseX > clipX2 || mouseY < clipY1 || mouseY > clipY2) {
            return null;
        }

        Map<String, SkillTreeNode> nodes =
                (Map<String, SkillTreeNode>) getFieldValue(treeObj, "nodes");
        if (nodes == null || nodes.isEmpty()) return null;

        float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();

        int pivotX = clipX1 + AREA_WIDTH / 2;
        int pivotY = clipY1 + AREA_HEIGHT / 2;

        int unscaledMouseX = (int)((mouseX - pivotX) / scale + pivotX);
        int unscaledMouseY = (int)((mouseY - pivotY) / scale + pivotY);

        for (SkillTreeNode n : nodes.values()) {
            if (n.containsPoint(unscaledMouseX, unscaledMouseY)) {
                return n; // больше не проверяем locked / learned
            }
        }

        return null;
    }
    /**
     * Проверяет завершение изучения узла без отрисовки полоски прогресса
     * Прогресс отображается только в тултипе
     */


    public static void renderHoldProgressOverlay(GuiGraphics gui, int mouseX, int mouseY) {
        if (currentHoveredNode == null || mousePressTime <= 0L) return;

        // Проверка: узел не должен быть изучен до max или заблокирован
        if (currentHoveredNode.isMaxLevel() || currentHoveredNode.locked) {
            mousePressTime = 0L;
            currentHoveredNode = null;
            return;
        }
        if (currentHoveredNode.variants != null && !currentHoveredNode.variants.isEmpty()
                && currentHoveredNode.selectedOption == -1) {
            // Показываем подсказку, что нужно выбрать вариант
            mousePressTime = 0L;
            currentHoveredNode = null;
            return;
        }
        Object treeObj = invokePrivateGetCurrentTree();
        if (treeObj == null) return;

        float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();

        int clipX1 = currentPanelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int clipY1 = currentPanelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
        int pivotX = clipX1 + AREA_WIDTH / 2;
        int pivotY = clipY1 + AREA_HEIGHT / 2;

        // переводим мышь в unscaled
        int unscaledMouseX = (int)((mouseX - pivotX) / scale + pivotX);
        int unscaledMouseY = (int)((mouseY - pivotY) / scale + pivotY);

        // если мышь ушла с ноды — сбрасываем удержание
        if (!currentHoveredNode.containsPoint(unscaledMouseX, unscaledMouseY)) {
            mousePressTime = 0L;
            currentHoveredNode = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Проверяем оба условия: опыт игрока И уровень вкладки
        int REQUIRED_LEVELS = 5;
        int treeId = getActiveTreeIdViaReflection();
        int playerTreeLevel = DamageBookRenderer.getLevel(treeId);

        if (mc.player.experienceLevel < REQUIRED_LEVELS ||
                currentHoveredNode.getRequiredTreeLevel() > playerTreeLevel) {

            triggerXpFailFlash(currentHoveredNode);
            mousePressTime = 0L;
            currentHoveredNode = null;
            return;
        }
        long now = System.currentTimeMillis();
        final long HOLD_MS = 1500L;
        float progress = Math.min(1f, (float)(now - mousePressTime) / HOLD_MS);

        // Завершение изучения
        if (progress >= 1f) {
            int activeTreeId = getActiveTreeIdViaReflection();

            // Новый синтаксис отправки пакета на сервер в NeoForge 1.21.1
            PacketDistributor.sendToServer(new LearnNodePacket(activeTreeId, currentHoveredNode.id));

            // Обновим расположение
            Object treeObj2 = invokePrivateGetCurrentTree();
            if (treeObj2 != null) {
                invokePrivateCalculateAndUpdatePositions(treeObj2, currentPanelScreenX, currentPanelScreenY);
            }

            mousePressTime = 0L;
            currentHoveredNode = null;
            return;
        }

        // Рисуем тултип с текущим уровнем и maxLevel
        Font font = mc.font;
        String baseKey = "damagecore.skilltree.node." + (currentHoveredNode.displayId != null ? currentHoveredNode.displayId : currentHoveredNode.id);

        // В 1.21.1 Component.translatable возвращает изменяемый компонент (MutableComponent)
        String title = Component.translatable(baseKey).getString();
        String desc = Component.translatable(baseKey + ".desc").getString();
        if (desc.equals(baseKey + ".desc")) desc = "";

        String titleWithLevel = title + " " + currentHoveredNode.level + "/" + currentHoveredNode.maxLevel;

        // --- Вычисляем ширину тултипа для полоски прогресса ---
        final int TEXT_MAX_PIXELS = 220;
        List<String> titleLines = RenderDrawUtils.splitStringToPixelWidth(font, titleWithLevel, TEXT_MAX_PIXELS);
        List<String> descLines = RenderDrawUtils.splitStringToPixelWidth(font, desc, TEXT_MAX_PIXELS);
        int maxWidth = 0;
        for (String s : titleLines) maxWidth = Math.max(maxWidth, font.width(s));
        for (String s : descLines)  maxWidth = Math.max(maxWidth, font.width(s));

        int frame = SkillTreeNode.FRAME_SIZE;
        int cellLeft = currentHoveredNode.x;
        int cellRight = currentHoveredNode.x + frame;
        int textStartX = cellRight + 4;
        int stripLeft = cellLeft - RenderDrawUtils.TOOLTIP_LEFT_OVERHANG;
        int stripWidth = (textStartX - stripLeft) + maxWidth + RenderDrawUtils.TOOLTIP_RIGHT_PAD;

        // Рисуем полоску прогресса с вычисленной шириной
        RenderDrawUtils.drawProgressBarUnderNode(gui, currentHoveredNode, progress, pivotX, pivotY, scale, stripWidth);

        // Рисуем сам тултип
        RenderDrawUtils.drawScaledTooltipWithProgress(gui, font, currentHoveredNode, titleWithLevel, desc, pivotX, pivotY, scale, progress);
    }

    private static int getActiveTreeIdViaReflection() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer");
            Field f = cls.getDeclaredField("activeTreeId");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Throwable t) {
            t.printStackTrace();
            return 0;
        }
    }

    // --- вспомогательные упрощения, не трогающие логику ---
    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int panelScreenX, int panelScreenY,
                              int mouseX, int mouseY) {

        try {
            Object treeObj = invokePrivateGetCurrentTree();
            if (treeObj == null) return;

            Map<String, SkillTreeNode> nodes =
                    (Map<String, SkillTreeNode>) getFieldValue(treeObj, "nodes");
            if (nodes == null || nodes.isEmpty()) return;

            currentPanelScreenX = panelScreenX;
            currentPanelScreenY = panelScreenY;

            float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();

            boolean isDragging = Boolean.TRUE.equals(getFieldValue(treeObj, "isDragging"));

            // --- drag ---
            if (isDragging) {
                int dragStartOffsetX = ((Number) getFieldValue(treeObj, "dragStartOffsetX")).intValue();
                int dragStartOffsetY = ((Number) getFieldValue(treeObj, "dragStartOffsetY")).intValue();
                int dragStartX = ((Number) getFieldValue(treeObj, "dragStartX")).intValue();
                int dragStartY = ((Number) getFieldValue(treeObj, "dragStartY")).intValue();

                int newOffsetX = dragStartOffsetX + (int) ((mouseX - dragStartX) / scale);
                int newOffsetY = dragStartOffsetY + (int) ((mouseY - dragStartY) / scale);

                setFieldValue(treeObj, "offsetX", newOffsetX);
                setFieldValue(treeObj, "offsetY", newOffsetY);
            }

            invokePrivateCalculateAndUpdatePositions(treeObj, panelScreenX, panelScreenY);

            // --- clip area ---
            int clipX1 = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
            int clipY1 = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
            int clipX2 = clipX1 + AREA_WIDTH;
            int clipY2 = clipY1 + AREA_HEIGHT;

            boolean scissorEnabled = false;
            if (clipX2 > clipX1 && clipY2 > clipY1) {
                gui.enableScissor(clipX1, clipY1, clipX2, clipY2);
                scissorEnabled = true;
            }

            PoseStack pose = gui.pose();
            int pivotX = clipX1 + AREA_WIDTH / 2;
            int pivotY = clipY1 + AREA_HEIGHT / 2;

            String activeOptionsNodeId = (String) getFieldValue(treeObj, "activeOptionsNodeId");
            boolean optionsOpen = activeOptionsNodeId != null;

            // NEW: если activeOptionsNodeId указывает на заблокированную ноду — закрываем варианты
            if (optionsOpen) {
                try {
                    SkillTreeNode activeNode = nodes.get(activeOptionsNodeId);
                    if (activeNode != null && activeNode.locked) {
                        // сбрасываем активный id — варианты не могут быть открыты для заблокированной ноды
                        setFieldValue(treeObj, "activeOptionsNodeId", null);
                        activeOptionsNodeId = null;
                        optionsOpen = false;
                    }
                } catch (Throwable ignored) {}
            }

            int unscaledMouseX = (int) ((mouseX - pivotX) / scale + pivotX);
            int unscaledMouseY = (int) ((mouseY - pivotY) / scale + pivotY);

            // =============================
            // 1) ЛИНИИ (всегда рисуются)
            // =============================
            pose.pushPose();
            pose.translate(pivotX, pivotY, 0);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            // Получаем кэш уровней (если доступен)
            int activeTreeId = getActiveTreeIdViaReflection();
            Map<String, Integer> levels = null;
            try {
                levels = SkillTreeClientSync.getLevelCache(activeTreeId);
            } catch (Throwable ignored) {}

            // Рисуем линии, выбирая цвет по состоянию child'а
            for (SkillTreeNode child : nodes.values()) {
                if (child.isRoot()) continue;

                for (String parentId : child.parentIds) {
                    if ("start".equalsIgnoreCase(parentId)) continue;
                    SkillTreeNode parent = nodes.get(parentId);
                    if (parent == null) continue;

                    int color;

                    int childLevel = (levels != null) ? levels.getOrDefault(child.id, child.level) : child.level;

                    if (childLevel >= 1) {  // Изучена хотя бы 1 раз
                        color = LINE_COLOR_LEARNED;
                    } else if (child.locked) {
                        color = LINE_COLOR_LOCKED;
                    } else {
                        // проверим — все ли родители имеют level >= 1
                        boolean allParentsAtLeastOne = true;
                        for (String pid : child.parentIds) {
                            if (pid == null || "start".equalsIgnoreCase(pid)) continue;
                            int plev = 0;
                            if (levels != null && levels.containsKey(pid)) {
                                Integer pv = levels.get(pid);
                                plev = (pv == null) ? 0 : pv;
                            } else {
                                SkillTreeNode pnode = nodes.get(pid);
                                if (pnode != null) plev = pnode.level;
                            }
                            if (plev <= 0) { allParentsAtLeastOne = false; break; }
                        }
                        color = allParentsAtLeastOne ? LINE_COLOR_LEARNABLE : LINE_COLOR_LOCKED;
                    }

                    RenderDrawUtils.drawThickLine(gui,
                            parent.centerX(), parent.centerY(),
                            child.centerX(), child.centerY(),
                            2, color);
                }
            }
            pose.popPose();

            // =============================
            // 2) ПОИСК HOVERed (только если варианты НЕ открыты)
            // =============================
            SkillTreeNode hoveredNode = null;
            RenderDrawUtils.OptionHoverInfo hoveredOption = null;

            // мышь внутри панели?
            boolean mouseInsidePanel =
                    mouseX >= clipX1 && mouseX <= clipX2 &&
                            mouseY >= clipY1 && mouseY <= clipY2;

            // ищем ноду ВСЕГДА (это нужно для drag системы)
            if (!optionsOpen) {
                for (SkillTreeNode n : nodes.values()) {
                    if (n.containsPoint(unscaledMouseX, unscaledMouseY)) {
                        hoveredNode = n;
                        break;
                    }
                }
            }

            // но hover активен только внутри панели
            if (!mouseInsidePanel) {
                hoveredNode = null;
            }

            currentHoveredNode = hoveredNode;

            // =============================
            // 3) ОТРИСОВКА УЗЛОВ (кроме hovered)
            // =============================
            pose.pushPose();
            pose.translate(pivotX, pivotY, 100);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            // В Render.render(), секция отрисовки узлов - ИСПРАВЛЕННЫЙ КОД
            for (SkillTreeNode n : nodes.values()) {
                boolean isActiveNode = optionsOpen && activeOptionsNodeId != null && activeOptionsNodeId.equals(n.id);

                // Для активного узла - всегда затемненный, без эффектов
                if (isActiveNode) {
                    RenderDrawUtils.drawNodeDimmed(gui, n);
                }
                // Для неактивных узлов при открытых опциях - тоже затемненные
                else if (optionsOpen) {
                    RenderDrawUtils.drawNodeDimmed(gui, n);
                }
                // Для закрытых опций - обычная отрисовка (кроме hovered, который рисуется позже)
                else if (n != hoveredNode) {
                    RenderDrawUtils.drawNode(gui, n, unscaledMouseX, unscaledMouseY);
                }

                // Рисуем опции для активного узла
                if (isActiveNode) {
                    RenderDrawUtils.OptionHoverInfo hi = RenderDrawUtils.drawOptions(gui, n, treeObj, unscaledMouseX, unscaledMouseY, false);
                    if (hi != null) hoveredOption = hi;
                }
            }
            pose.popPose();

            // =============================
            // 4) hovered узел поверх остальных (только если варианты закрыты)
            // =============================
            if (!optionsOpen && hoveredNode != null) {
                pose.pushPose();
                pose.translate(pivotX, pivotY, 1000);
                pose.scale(scale, scale, 1f);
                pose.translate(-pivotX, -pivotY, 0);

                RenderDrawUtils.drawNode(gui, hoveredNode, unscaledMouseX, unscaledMouseY);

                pose.popPose();

                // рисуем красную мигалку ВНЕ внутреннего трансформ-блока,
                // точно так же, как рисуется зелёный прогресс ниже.
                renderXpFailFlash(gui, hoveredNode);
            }

            // =============================
            // ОТКЛЮЧАЕМ SCISSOR ПЕРЕД ПРОГРЕСС-БАРОМ И ТУЛТИПАМИ
            // =============================
            if (scissorEnabled) {
                gui.disableScissor();
            }

            if (!optionsOpen && hoveredNode != null) {
                renderHoldProgressOverlay(gui, mouseX, mouseY);
            }

            // =============================
            // 5) ДОПОЛНИТЕЛЬНОЕ ЗАТЕМНЕНИЕ (только если варианты открыты)
            // =============================
            if (optionsOpen) {
                // Затемняем весь экран поверх всего (кроме опций)
                gui.fill(0, 0, screen.width, screen.height, 0x88000000);
            }

            // =============================
            // 6) ТУЛТИП ДЛЯ ОПЦИИ (всегда, если есть наведенная опция)
            // =============================
            if (hoveredOption != null) {
                Font font = Minecraft.getInstance().font;
                String baseKey = "damagecore.skilltree.variant." + hoveredOption.variant.displayId;
                String title = Component.translatable(baseKey).getString();
                String desc  = Component.translatable(baseKey + ".desc").getString();
                if (desc.equals(baseKey + ".desc")) desc = "";

                SkillTreeNode tempNode = new SkillTreeNode("temp", ItemStack.EMPTY, false, new ArrayList<>(), SkillTreeNode.Side.RIGHT);
                tempNode.x = hoveredOption.centerX - SkillTreeNode.FRAME_SIZE/2;
                tempNode.y = hoveredOption.centerY - SkillTreeNode.FRAME_SIZE/2;

                // Передаём true для isVariant
                RenderDrawUtils.drawScaledTooltip(gui, font, tempNode, title, desc, pivotX, pivotY, scale, true);
            }

            // =============================
            // 7) ТУЛТИП ДЛЯ УЗЛА (только если варианты закрыты)
            // =============================
            if (!optionsOpen && hoveredNode != null) {
                Font font = Minecraft.getInstance().font;
                String baseKey = "damagecore.skilltree.node." + (hoveredNode.displayId != null ? hoveredNode.displayId : hoveredNode.id);
                String title = Component.translatable(baseKey).getString();
                String desc = Component.translatable(baseKey + ".desc").getString();
                if (desc.equals(baseKey + ".desc")) desc = "";

                // используем существующие activeTreeId и levels
                int nodeLevel = levels != null ? levels.getOrDefault(hoveredNode.id, hoveredNode.level) : hoveredNode.level;
                String titleWithLevel = title + " " + nodeLevel + "/" + hoveredNode.maxLevel;

                // прогресс удержания
                float progress = 0f;
                if (hoveredNode == currentHoveredNode && mousePressTime > 0L) {
                    long now = System.currentTimeMillis();
                    final long HOLD_MS = 1500L;
                    progress = Math.min(1f, (float)(now - mousePressTime) / HOLD_MS);
                } else if (hoveredNode.isMaxLevel()) {
                    progress = 1f;
                }

                RenderDrawUtils.drawScaledTooltipWithProgress(gui, font, hoveredNode, titleWithLevel, desc, pivotX, pivotY, scale, progress);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    // ----------------- Рефлексия / вспомогательные методы -----------------
    public static Object invokePrivateGetCurrentTree() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer");
            Method m = cls.getDeclaredMethod("getCurrentTree");
            m.setAccessible(true);
            return m.invoke(null);
        } catch (NoSuchMethodException e) {
            // fallback: попробуем получить trees + activeTreeId
            try {
                Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer");
                Field treesField = cls.getDeclaredField("trees");
                Field activeField = cls.getDeclaredField("activeTreeId");
                treesField.setAccessible(true);
                activeField.setAccessible(true);
                Object treesObj = treesField.get(null);
                int activeId = activeField.getInt(null);
                if (treesObj instanceof Map) {
                    Map<?, ?> trees = (Map<?, ?>) treesObj;
                    return trees.get(activeId);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void invokePrivateCalculateAndUpdatePositions(Object treeObj, int panelScreenX, int panelScreenY) {
        try {
            Class<?> renderClass = Class.forName("ru.imaginaerum.damagecore.api.skill_tree.SkillTreeRenderer");

            // ИСПРАВЛЕНИЕ: проверяем сигнатуру метода - возможно нужна передача scale
            // Пробуем разные варианты сигнатур
            try {
                // Вариант 1: без scale
                Method m = renderClass.getDeclaredMethod("calculateAndUpdatePositions",
                        treeObj.getClass(), int.class, int.class);
                m.setAccessible(true);
                m.invoke(null, treeObj, panelScreenX, panelScreenY);
            } catch (NoSuchMethodException e1) {
                try {
                    // Вариант 2: с scale
                    Method m = renderClass.getDeclaredMethod("calculateAndUpdatePositions",
                            treeObj.getClass(), int.class, int.class, float.class);
                    m.setAccessible(true);

                    // Получаем текущий scale
                    float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();
                    m.invoke(null, treeObj, panelScreenX, panelScreenY, scale);
                } catch (NoSuchMethodException e2) {
                    // Игнорируем
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException nsf) {
            // пробуем у супер-класса (на случай внутренних реализаций)
            Class<?> c = obj.getClass();
            while ((c = c.getSuperclass()) != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException ignored) {}
                catch (Throwable ex) { ex.printStackTrace(); return null; }
            }
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null) return;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (NoSuchFieldException nsf) {
            Class<?> c = obj.getClass();
            while ((c = c.getSuperclass()) != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(obj, value);
                    return;
                } catch (NoSuchFieldException ignored) {}
                catch (Throwable ex) { ex.printStackTrace(); return; }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}