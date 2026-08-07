package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact but behavior-preserving RenderDrawUtils.
 * All rendering logic intentionally preserved (colors, Z-order, progress behavior).
 */
public class RenderDrawUtils {
    private static final int OPTION_BASE_RADIUS = 36;
    private static final ResourceLocation TOOLTIP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");
    private static final ResourceLocation SKILL_CIRCLE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/container/creative_inventory/d_c_skill_circle.png");
    private static final ResourceLocation SKILL_BG =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/container/creative_inventory/d_c_skill_bg.png");

    private static final int TOOLTIP_TITLE_SRC_U = 0;
    private static final int TOOLTIP_DESC_SRC_U  = 0;
    private static final int TOOLTIP_DESC_SRC_V  = 294;
    private static final int TOOLTIP_SRC_W = 200;
    private static final int TOOLTIP_SRC_H = 20;
    private static final int TOOLTIP_CAP = 4;
    public static final int TOOLTIP_LEFT_OVERHANG = 4;
    public static final int TOOLTIP_RIGHT_PAD = 4;
    private static final int TOOLTIP_TITLE_GREEN_V = 252;
    private static final int TOOLTIP_TITLE_YELLOW_V = 273;
    private static final int TOOLTIP_TITLE_WHITE_V  = 314;
    private static final int DESC_PADDING = 3;

    public static final float Z_NODE_TOP = 450f;
    private static final float Z_LINES = 100f;
    private static final float Z_OPTIONS_BG = 330f;
    private static final float Z_NODE = 50f;
    private static final float Z_OPTIONS = 400f;
    private static final float Z_TOOLTIP_DESC_BG = 460f;
    private static final float Z_TOOLTIP_DESC_TEXT = 470f;
    private static final float Z_TOOLTIP_TITLE_BG = 480f;
    private static final float Z_TOOLTIP_TITLE_TEXT = 490f;
    private static final int PROGRESS_BAR_WIDTH = 158;
    private static final int PROGRESS_BAR_HEIGHT = 5;
    private static final int PROGRESS_BAR_EMPTY_U = 0;
    private static final int PROGRESS_BAR_EMPTY_V = 342;
    private static final int PROGRESS_BAR_FILLED_U = 0;
    private static final int PROGRESS_BAR_FILLED_V = 336;
    public static void blitTex(GuiGraphics gui, ResourceLocation tex, int x, int y, int u, int v, int w, int h) {
        gui.blit(tex, x, y, u, v, w, h, 512, 512);
    }

    // --- public entry points keep original semantics exactly ---
    public static void drawScaledTooltipWithProgress(GuiGraphics gui, Font font, SkillTreeNode n,
                                                     String title, String desc, int pivotX, int pivotY,
                                                     float scale, float serverProgress) {
        // serverProgress игнорируем - прогресс больше не рисуем в тултипе
        final int TEXT_MAX_PIXELS = 220;
        List<String> titleLines = splitStringToPixelWidth(font, title, TEXT_MAX_PIXELS);
        List<String> descLines  = splitStringToPixelWidth(font, desc, TEXT_MAX_PIXELS);
        if (titleLines.isEmpty() && descLines.isEmpty()) return;

        int frame = SkillTreeNode.FRAME_SIZE;
        int maxWidth = 0;
        for (String s : titleLines) maxWidth = Math.max(maxWidth, font.width(s));
        for (String s : descLines)  maxWidth = Math.max(maxWidth, font.width(s));

        int cellLeft = (n != null) ? n.x : 0;
        int cellRight = (n != null) ? (n.x + frame) : frame;
        int cellCenterY = (n != null) ? (n.y + frame / 2) : (frame / 2);

        int stripLeft = cellLeft - TOOLTIP_LEFT_OVERHANG;
        int textStartX = cellRight + 4;
        int stripWidth = (textStartX - stripLeft) + maxWidth + TOOLTIP_RIGHT_PAD;

        int titleHeight = Math.max(TOOLTIP_SRC_H, titleLines.size() * font.lineHeight + 6);
        int descHeight  = descLines.isEmpty() ? 0 : Math.max(TOOLTIP_SRC_H, descLines.size() * font.lineHeight + 4);
        int titleTop = cellCenterY - titleHeight / 2;
        int descTop = titleTop + titleHeight - 4;

        // базовый цвет заголовка — теперь жёлтый при наличии вариантов имеет приоритет над learned
        int titleSrcV;
        if (n != null && n.variants != null && !n.variants.isEmpty()) {
            titleSrcV = TOOLTIP_TITLE_YELLOW_V; // варианты — всегда жёлтый (даже если изучена)
        } else if (n != null && n.isLearned()) {
            titleSrcV = TOOLTIP_TITLE_GREEN_V; // изучена
        } else {
            titleSrcV = TOOLTIP_TITLE_WHITE_V; // дефолт
        }

        PoseStack pose = gui.pose();

        // description background
        if (descHeight > 0) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_DESC_BG, scale);
            drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, descTop, stripWidth, descHeight + 6,
                    TOOLTIP_DESC_SRC_U, TOOLTIP_DESC_SRC_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
            popTransform(pose);
        }

        // title background (без зелёного прогресса!)
        pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_BG, scale);
        drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, stripWidth, titleHeight,
                TOOLTIP_TITLE_SRC_U, titleSrcV, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
        popTransform(pose);

        // описание текста
        if (!descLines.isEmpty()) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_DESC_TEXT, scale);
            int descTextY = descTop + DESC_PADDING + 4;
            for (String s : descLines) {
                gui.drawString(font, s, stripLeft + 4, descTextY, 0xFFE0E0E0, false);
                descTextY += font.lineHeight;
            }
            popTransform(pose);
        }

        // текст заголовка
        pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_TEXT, scale);
        int titleTextY = titleTop + (titleHeight - titleLines.size() * font.lineHeight) / 2;
        for (String s : titleLines) {
            gui.drawString(font, s, textStartX, titleTextY, 0xFFFFFFFF, true);
            titleTextY += font.lineHeight;
        }
        popTransform(pose);
    }

    private static final int PROGRESS_BAR_CAP = 2; // скругление углов полоски

    public static void drawProgressBarUnderNode(GuiGraphics gui, SkillTreeNode node, float progress, int pivotX, int pivotY, float scale, int tooltipWidth) {
        if (node == null || progress <= 0f) return;

        int frameSize = SkillTreeNode.FRAME_SIZE;
        // Полоска начинается сразу от правого края ноды
        int barX = node.x + frameSize;
        int barY = node.y + frameSize - PROGRESS_BAR_HEIGHT;

        // Вычисляем правый край тултипа (используем те же отступы, что и в тултипе)
        int stripLeft = node.x - TOOLTIP_LEFT_OVERHANG;
        int rightEdge = stripLeft + tooltipWidth;

        // Ширина полоски = расстояние от правого края ноды до правого края тултипа
        int width = rightEdge - barX;
        if (width <= 0) return; // Не рисуем, если полоска не умещается

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(pivotX, pivotY, 1000f);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        // Пустая шкала (растягивается с помощью nine-slice)
        drawNineSliceTiled(gui, TOOLTIP_TEXTURE,
                barX, barY, width, PROGRESS_BAR_HEIGHT,
                PROGRESS_BAR_EMPTY_U, PROGRESS_BAR_EMPTY_V, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                PROGRESS_BAR_CAP);

        // Заполненная часть
        if (progress > 0f) {
            int filledWidth = (int)(width * Math.min(progress, 1f));
            if (filledWidth > 0) {
                drawNineSliceTiled(gui, TOOLTIP_TEXTURE,
                        barX, barY, filledWidth, PROGRESS_BAR_HEIGHT,
                        PROGRESS_BAR_FILLED_U, PROGRESS_BAR_FILLED_V, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                        PROGRESS_BAR_CAP);
            }
        }
        pose.popPose();
    }

    public static void drawScaledTooltip(GuiGraphics gui, Font font, SkillTreeNode n,
                                         String title, String desc, int pivotX, int pivotY,
                                         float scale, boolean isVariant) {
        drawTooltip(gui, font, n, title, desc, pivotX, pivotY, scale, isVariant, /*forceVariantYellowIfNodeHasVariants=*/false, /*progress=*/0f);
    }

    // --- unified implementation but keeps both original selection rules ---
    private static void drawTooltip(GuiGraphics gui, Font font, SkillTreeNode n,
                                    String title, String desc, int pivotX, int pivotY,
                                    float scale, boolean isVariant, boolean forceVariantYellowIfNodeHasVariants, float progress) {
        final int TEXT_MAX_PIXELS = 220;
        List<String> titleLines = splitStringToPixelWidth(font, title, TEXT_MAX_PIXELS);
        List<String> descLines  = splitStringToPixelWidth(font, desc, TEXT_MAX_PIXELS);
        if (titleLines.isEmpty() && descLines.isEmpty()) return;

        int frame = SkillTreeNode.FRAME_SIZE;
        int maxWidth = 0; for (String s : titleLines) maxWidth = Math.max(maxWidth, font.width(s));
        for (String s : descLines)  maxWidth = Math.max(maxWidth, font.width(s));

        int cellLeft   = (n != null) ? n.x : 0;
        int cellRight  = (n != null) ? (n.x + frame) : frame;
        int cellCenterY = (n != null) ? (n.y + frame / 2) : (frame / 2);

        int stripLeft = cellLeft - TOOLTIP_LEFT_OVERHANG;
        int textStartX = cellRight + 4;
        int stripWidth = (textStartX - stripLeft) + maxWidth + TOOLTIP_RIGHT_PAD;

        int titleHeight = Math.max(TOOLTIP_SRC_H, titleLines.size() * font.lineHeight + 6);
        int descHeight  = descLines.isEmpty() ? 0 : Math.max(TOOLTIP_SRC_H, descLines.size() * font.lineHeight + 4);
        int titleTop = cellCenterY - titleHeight / 2;
        int descTop = titleTop + titleHeight - 4;

        // выбор базового цвета — ТОНКОЕ место (мы соблюдаем исходное поведение):
        // - В drawScaledTooltipWithProgress (forceVariantYellowIfNodeHasVariants==true) —
        //   если у ноды есть варианты -> всегда жёлтый (даже если learned).
        // - В drawScaledTooltip (isVariant param) — isVariant имеет приоритет и делает жёлтым.
        int titleSrcV;
        if (forceVariantYellowIfNodeHasVariants && n != null && n.variants != null && !n.variants.isEmpty()) {
            titleSrcV = TOOLTIP_TITLE_YELLOW_V;
        } else if (isVariant) {
            titleSrcV = TOOLTIP_TITLE_YELLOW_V;
        } else if (n != null && n.isLearned()) {
            titleSrcV = TOOLTIP_TITLE_GREEN_V;
        } else if (n != null && n.variants != null && !n.variants.isEmpty()) {
            // preserve original: a node *with variants* normally shows yellow in some flows;
            // drawScaledTooltipWithProgress already forced that above; here we keep same fallback.
            titleSrcV = TOOLTIP_TITLE_YELLOW_V;
        } else {
            titleSrcV = TOOLTIP_TITLE_WHITE_V;
        }

        PoseStack pose = gui.pose();
// green progress overlay for non-learned nodes when progress>0
        if (progress > 0f && n != null && !n.isLearned()) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_BG + 5, scale);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            int greenWidth = Math.round(stripWidth * Math.min(progress, 1f));
            drawPartialProgressTooltip(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, greenWidth, titleHeight,
                    TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_GREEN_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);

            RenderSystem.disableBlend();
            popTransform(pose);
        }
        // description background (below)
        if (descHeight > 0) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_DESC_BG, scale);
            drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, descTop, stripWidth, descHeight + 6,
                    TOOLTIP_DESC_SRC_U, TOOLTIP_DESC_SRC_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
            popTransform(pose);
        }

        // title base background
        pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_BG, scale);
        drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, stripWidth, titleHeight,
                TOOLTIP_TITLE_SRC_U, titleSrcV, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
        popTransform(pose);

        // green progress overlay for non-learned nodes when progress>0
        if (progress > 0 && n != null && !n.isLearned()) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_BG + 5, scale);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            if (progress >= 1.0f) {
                drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, stripWidth, titleHeight,
                        TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_GREEN_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
            } else {
                int greenWidth = Math.round(stripWidth * progress);
                drawPartialProgressTooltip(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, greenWidth, titleHeight,
                        TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_GREEN_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
            }

            RenderSystem.disableBlend();
            popTransform(pose);
        }

        // description text
        if (!descLines.isEmpty()) {
            pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_DESC_TEXT, scale);
            int descTextY = descTop + DESC_PADDING + 4;
            for (String s : descLines) {
                gui.drawString(font, s, stripLeft + 4, descTextY, 0xFFE0E0E0, false);
                descTextY += font.lineHeight;
            }
            popTransform(pose);
        }

        // title text (on top)
        pushTransform(pose, pivotX, pivotY, Z_TOOLTIP_TITLE_TEXT, scale);
        int titleTextY = titleTop + (titleHeight - titleLines.size() * font.lineHeight) / 2;
        for (String s : titleLines) {
            gui.drawString(font, s, textStartX, titleTextY, 0xFFFFFFFF, true);
            titleTextY += font.lineHeight;
        }
        popTransform(pose);
    }

    private static void pushTransform(PoseStack pose, int pivotX, int pivotY, float z, float scale) {
        pose.pushPose();
        pose.translate(pivotX, pivotY, z);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);
    }
    private static void popTransform(PoseStack pose) { pose.popPose(); }

    private static void drawPartialProgressTooltip(GuiGraphics gui, ResourceLocation tex,
                                                   int destX, int destY, int destW, int destH,
                                                   int srcU, int srcV, int srcW, int srcH,
                                                   int cap) {
        if (destW <= 0 || destH <= 0) return;
        if (destW <= cap) {
            blitTex(gui, tex, destX, destY, srcU, srcV, destW, Math.min(cap, destH));
            if (destH > cap) blitTex(gui, tex, destX, destY + destH - cap, srcU, srcV + srcH - cap, destW, cap);
            return;
        }

        int innerSrcW = srcW - cap * 2, innerSrcH = srcH - cap * 2;
        blitTex(gui, tex, destX, destY, srcU, srcV, cap, cap);
        if (destH > cap) blitTex(gui, tex, destX, destY + destH - cap, srcU, srcV + srcH - cap, cap, cap);

        int x = destX + cap, maxX = destX + destW, remainingWidth = destW - cap;
        while (x < maxX && remainingWidth > 0) {
            int tileW = Math.min(innerSrcW, remainingWidth);
            blitTex(gui, tex, x, destY, srcU + cap, srcV, tileW, cap);
            blitTex(gui, tex, x, destY + destH - cap, srcU + cap, srcV + srcH - cap, tileW, cap);
            x += tileW;
            remainingWidth -= tileW;
        }

        if (destH > cap) { x = destX + cap; remainingWidth = destW - cap; while (x < maxX && remainingWidth > 0) { int tileW = Math.min(innerSrcW, remainingWidth); blitTex(gui, tex, x, destY + destH - cap, srcU + cap, srcV + srcH - cap, tileW, cap); x += tileW; remainingWidth -= tileW; } }

        if (destH > cap * 2) { int y = destY + cap, maxY = destY + destH - cap; while (y < maxY) { int tileH = Math.min(innerSrcH, maxY - y); blitTex(gui, tex, destX, y, srcU, srcV + cap, cap, tileH); y += tileH; } }

        if (destW > cap && destH > cap * 2) {
            int y = destY + cap, maxY = destY + destH - cap;
            while (y < maxY) {
                int tileH = Math.min(innerSrcH, maxY - y);
                x = destX + cap; remainingWidth = destW - cap;
                while (x < maxX && remainingWidth > 0) {
                    int tileW = Math.min(innerSrcW, remainingWidth);
                    blitTex(gui, tex, x, y, srcU + cap, srcV + cap, tileW, tileH);
                    x += tileW; remainingWidth -= tileW;
                }
                y += tileH;
            }
        }
    }

    public static void drawNodeDimmed(GuiGraphics gui, SkillTreeNode n) {
        int frameSize = SkillTreeNode.FRAME_SIZE, padding = SkillTreeNode.FRAME_PADDING, ITEM_SIZE = 16;
        PoseStack pose = gui.pose(); pose.pushPose(); pose.translate(0,0,Z_NODE);
        gui.fill(n.x, n.y, n.x + frameSize, n.y + frameSize, 0xFF222222);
        gui.fill(n.x + padding, n.y + padding, n.x + frameSize - padding, n.y + frameSize - padding, 0xFF444444);
        int itemX = n.x + (frameSize - ITEM_SIZE)/2, itemY = n.y + (frameSize - ITEM_SIZE)/2;
        gui.renderItem(n.itemStack, itemX, itemY);
        gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);
        pose.popPose();
    }

    public static class OptionHoverInfo {
        public final SkillTreeNode node;
        public final SkillTreeNode.Variant variant;
        public final int optionIndex, centerX, centerY;
        public OptionHoverInfo(SkillTreeNode node, SkillTreeNode.Variant variant, int optionIndex, int centerX, int centerY) {
            this.node = node; this.variant = variant; this.optionIndex = optionIndex; this.centerX = centerX; this.centerY = centerY;
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static OptionHoverInfo drawOptions(GuiGraphics gui, SkillTreeNode node, Object treeObj, int mouseX, int mouseY, boolean canLearn) {
        try {
            if (node == null || node.locked) return null;
            List<?> opts = (List<?>) getFieldValue(node, "options");
            if (opts == null || opts.isEmpty()) return null;

            final int OPTION_SIZE = 27, ITEM_SIZE = 16;
            OptionHoverInfo hoveredInfo = null;
            PoseStack pose = gui.pose();
            int cx = node.centerX(), cy = node.centerY();

            int mainCircleSize = OPTION_BASE_RADIUS * 2 + OPTION_SIZE;
            pose.pushPose(); pose.translate(0,0,Z_OPTIONS_BG);
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false); RenderSystem.disableDepthTest();
            gui.blit(SKILL_CIRCLE, cx - mainCircleSize/2, cy - mainCircleSize/2, 0,0, mainCircleSize, mainCircleSize, mainCircleSize, mainCircleSize);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
            pose.popPose();

            for (int i = 0; i < opts.size(); i++) {
                int[] pos = optionCenterForIndex(node, i); int ox = pos[0], oy = pos[1];
                boolean hovered = mouseX >= ox - OPTION_SIZE/2 && mouseX < ox + OPTION_SIZE/2 && mouseY >= oy - OPTION_SIZE/2 && mouseY < oy + OPTION_SIZE/2;

                pose.pushPose(); pose.translate(0,0,Z_OPTIONS);
                RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.depthMask(false); RenderSystem.disableDepthTest();

                pose.pushPose(); pose.translate(ox - OPTION_SIZE/2f, oy - OPTION_SIZE/2f, 0);
                gui.blit(SKILL_CIRCLE,0,0,0,0, OPTION_SIZE, OPTION_SIZE, OPTION_SIZE, OPTION_SIZE);
                gui.blit(SKILL_BG,0,0,0,0, OPTION_SIZE, OPTION_SIZE, OPTION_SIZE, OPTION_SIZE);
                pose.popPose();

                RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();

                Object opt = opts.get(i);
                if (opt instanceof net.minecraft.world.item.ItemStack stack) {
                    int itemX = ox - ITEM_SIZE/2, itemY = oy - ITEM_SIZE/2;
                    pose.pushPose(); pose.translate(0,0,1); gui.renderItem(stack, itemX, itemY); gui.renderItemDecorations(Minecraft.getInstance().font, stack, itemX, itemY); pose.popPose();
                }

                if (hovered) {
                    RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    gui.fill(ox - OPTION_SIZE/2 + 1, oy - OPTION_SIZE/2 + 1, ox + OPTION_SIZE/2 - 1, oy + OPTION_SIZE/2 - 1, 0x80FFFFFF);
                    RenderSystem.disableBlend();
                }

                pose.popPose();

                pose.pushPose(); pose.translate(0,0,Z_NODE_TOP);
                RenderSystem.disableBlend(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
                // draw node with canLearn flag so it will blink if tree-XP missing
                drawNode(gui, node, mouseX, mouseY, canLearn);
                pose.popPose();

                if (hovered && hoveredInfo == null) {
                    node.applyVariant(i);
                    node.optionsVisible = false;
                    SkillTreeNode.Variant variant = (node.variants != null && i < node.variants.size()) ? node.variants.get(i) : null;
                    hoveredInfo = new OptionHoverInfo(node, variant, i, ox, oy);
                }
            }
            return hoveredInfo;
        } catch (Throwable t) { t.printStackTrace(); return null; }
    }

    public static List<String> splitLongWord(Font font, String w, int maxWidth) {
        List<String> parts = new ArrayList<>(); if (w == null || w.isEmpty()) return parts;
        StringBuilder part = new StringBuilder();
        for (char c : w.toCharArray()) {
            if (font.width(part.toString() + c) <= maxWidth) part.append(c);
            else { if (part.length() > 0) parts.add(part.toString()); part = new StringBuilder(); part.append(c); }
        }
        if (part.length() > 0) parts.add(part.toString());
        return parts;
    }

    public static List<String> splitStringToPixelWidth(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>(); if (text == null) return lines;
        text = text.trim(); if (text.isEmpty()) return lines;
        String[] words = text.split("\\s+"); StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() == 0) {
                if (font.width(w) > maxWidth) lines.addAll(splitLongWord(font, w, maxWidth));
                else cur.append(w);
            } else {
                String candidate = cur.toString() + ' ' + w;
                if (font.width(candidate) <= maxWidth) cur.append(' ').append(w);
                else {
                    lines.add(cur.toString());
                    if (font.width(w) > maxWidth) { lines.addAll(splitLongWord(font, w, maxWidth)); cur = new StringBuilder(); }
                    else cur = new StringBuilder(w);
                }
            }
        }
        if (cur.length() > 0) lines.add(cur.toString()); return lines;
    }

    public static void drawNineSliceTiled(GuiGraphics gui, ResourceLocation tex,
                                          int destX, int destY, int destW, int destH,
                                          int srcU, int srcV, int srcW, int srcH,
                                          int cap) {
        if (destW <= 0 || destH <= 0) return;
        if (destW <= cap * 2 || destH <= cap * 2) { blitTex(gui, tex, destX, destY, srcU, srcV, destW, destH); return; }

        int innerSrcW = srcW - cap * 2, innerSrcH = srcH - cap * 2;
        blitTex(gui, tex, destX, destY, srcU, srcV, cap, cap);
        blitTex(gui, tex, destX + destW - cap, destY, srcU + srcW - cap, srcV, cap, cap);
        blitTex(gui, tex, destX, destY + destH - cap, srcU, srcV + srcH - cap, cap, cap);
        blitTex(gui, tex, destX + destW - cap, destY + destH - cap, srcU + srcW - cap, srcV + srcH - cap, cap, cap);

        int x = destX + cap;
        while (x < destX + destW - cap) {
            int tileW = Math.min(innerSrcW, destX + destW - cap - x);
            blitTex(gui, tex, x, destY, srcU + cap, srcV, tileW, cap);
            blitTex(gui, tex, x, destY + destH - cap, srcU + cap, srcV + srcH - cap, tileW, cap);
            x += tileW;
        }

        int y = destY + cap;
        while (y < destY + destH - cap) {
            int tileH = Math.min(innerSrcH, destY + destH - cap - y);
            blitTex(gui, tex, destX, y, srcU, srcV + cap, cap, tileH);
            blitTex(gui, tex, destX + destW - cap, y, srcU + srcW - cap, srcV + cap, cap, tileH);
            y += tileH;
        }

        y = destY + cap;
        while (y < destY + destH - cap) {
            int tileH = Math.min(innerSrcH, destY + destH - cap - y);
            x = destX + cap;
            while (x < destX + destW - cap) {
                int tileW = Math.min(innerSrcW, destX + destW - cap - x);
                blitTex(gui, tex, x, y, srcU + cap, srcV + cap, tileW, tileH);
                x += tileW;
            }
            y += tileH;
        }
    }

    // overload: legacy / compatibility -- delegates to new method with canLearn=true
    public static void drawNode(GuiGraphics gui, SkillTreeNode n, int mouseX, int mouseY) {
        drawNode(gui, n, mouseX, mouseY, true);
    }

    // new: accepts canLearn - if false and hovered -> blink red (same effect as XP-fail)
    // В классе RenderDrawUtils, метод drawNode (примерно строка 244)

    public static void drawNode(GuiGraphics gui, SkillTreeNode n, int mouseX, int mouseY, boolean canLearn) {
        int frameSize = SkillTreeNode.FRAME_SIZE, padding = SkillTreeNode.FRAME_PADDING, ITEM_SIZE = 16;
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0,0,Z_NODE);

        // Определяем цвет рамки (внешней обводки) в зависимости от состояния узла
        int borderColor;
        // СНАЧАЛА проверяем наличие вариантов (они имеют приоритет)
        if (n.variants != null && !n.variants.isEmpty()) {
            borderColor = 0xFFFFFF66; // Желтый (как заголовок узла с вариантами) - даже если изучен
        } else if (n.isLearned()) {
            borderColor = 0xFF95E674; // лесной (как изученный заголовок)
        } else if (n.locked) {
            borderColor = 0xFF333333; // Темно-серый для заблокированных
        } else {
            borderColor = 0xFFDDDDDD; // Светло-серый/белый для доступных (как обычный заголовок)
        }

        // Рисуем внешнюю рамку цветом состояния
        int borderThickness = 3;
        // Верх
        gui.fill(n.x, n.y, n.x + frameSize, n.y + borderThickness, borderColor);
        // Низ
        gui.fill(n.x, n.y + frameSize - borderThickness, n.x + frameSize, n.y + frameSize, borderColor);
        // Лево
        gui.fill(n.x, n.y + borderThickness, n.x + borderThickness, n.y + frameSize - borderThickness, borderColor);
        // Право
        gui.fill(n.x + frameSize - borderThickness, n.y + borderThickness, n.x + frameSize, n.y + frameSize - borderThickness, borderColor);

        // Внутренняя заливка (немного темнее для контраста)
        int innerLeft = n.x + padding;
        int innerTop = n.y + padding;
        int innerRight = n.x + frameSize - padding;
        int innerBottom = n.y + frameSize - padding;

        boolean hovered = n.containsPoint(mouseX, mouseY);

        int innerColor;
            innerColor = hovered ? (n.locked ? 0xFF444444 : 0xFFDDDDDD) : 0xFF777777;
            gui.fill(innerLeft, innerTop, innerRight, innerBottom, innerColor);


        int itemX = n.x + (frameSize - ITEM_SIZE)/2, itemY = n.y + (frameSize - ITEM_SIZE)/2;
        gui.renderItem(n.itemStack, itemX, itemY);
        gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);

        pose.popPose();
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException nsf) {
            Class<?> c = obj.getClass();
            while ((c = c.getSuperclass()) != null) {
                try { Field f = c.getDeclaredField(fieldName); f.setAccessible(true); return f.get(obj); }
                catch (NoSuchFieldException ignored) {}
                catch (Throwable ex) { ex.printStackTrace(); return null; }
            }
            return null;
        } catch (Throwable t) { t.printStackTrace(); return null; }
    }

    private static int[] optionCenterForIndex(SkillTreeNode node, int index) {
        try {
            List<?> opts = (List<?>) getFieldValue(node, "options");
            int n = (opts == null) ? 0 : opts.size();
            if (n == 0) return new int[]{node.centerX(), node.centerY()};
            int radius = OPTION_BASE_RADIUS;
            double angle = 2.0 * Math.PI * index / n;
            int cx = node.centerX() + (int)Math.round(radius * Math.cos(angle));
            int cy = node.centerY() + (int)Math.round(radius * Math.sin(angle));
            return new int[]{cx, cy};
        } catch (Throwable t) { t.printStackTrace(); return new int[]{node.centerX(), node.centerY()}; }
    }

    public static void drawThickLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int thickness, int color) {
        if (y1 == y2) { gui.fill(Math.min(x1,x2), y1 - thickness/2, Math.max(x1,x2), y1 + thickness/2 + 1, color); return; }
        if (x1 == x2) { gui.fill(x1 - thickness/2, Math.min(y1,y2), x1 + thickness/2 + 1, Math.max(y1,y2), color); return; }
        int dx = x2 - x1, dy = y2 - y1, steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i=0;i<=steps;i++){ float t = i/(float)steps; int px = Math.round(x1 + t*dx), py = Math.round(y1 + t*dy);
            gui.fill(px - thickness/2, py - thickness/2, px + thickness/2 + 1, py + thickness/2 + 1, color);
        }
    }
}