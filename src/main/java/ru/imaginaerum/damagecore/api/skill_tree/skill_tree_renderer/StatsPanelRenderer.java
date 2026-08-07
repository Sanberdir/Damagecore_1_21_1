package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

import java.util.Arrays;

/**
 * Рендер кастомной панели характеристик (фон, полоска скролла,
 * строки статов с кнопками +/-) для InventoryScreen.
 *
 * Вынесено из InventoryScreenMixin, чтобы не раздувать миксин логикой отрисовки.
 * Состояние (offset скролла, активная боковая вкладка и т.п.) по-прежнему
 * хранится в миксине и передаётся сюда параметрами.
 */
public final class StatsPanelRenderer {

    private StatsPanelRenderer() {}

    public static final ResourceLocation DAMAGE_CORE_INTERFACE =
            ResourceLocation.fromNamespaceAndPath("damagecore",
                    "textures/gui/container/creative_inventory/damage_core_interface.png");

    private static final int ATLAS_SIZE = 512;

    // --- фон ---
    private static final int INV_BG_U = 0;
    private static final int INV_BG_V = 0;
    private static final int INV_BG_W = 176;
    private static final int INV_BG_H = 166;

    // --- строки статов ---
    private static final int ROW_H    = 8;
    private static final int ROW_STEP = ROW_H + 5;

    public static final int ROWS_TOTAL    = StatsType.values().length;
    public static final int ROWS_VISIBLE  = Math.min(4, ROWS_TOTAL);
    public static final int SCROLL_MAX_PX = (ROWS_TOTAL - ROWS_VISIBLE) * ROW_STEP;

    // --- полоска скролла ---
    private static final int STRIP_U = 176;
    private static final int STRIP_V = 225;
    private static final int STRIP_W = 5;
    private static final int STRIP_H = 15;
    private static final int STRIP_X = 165;
    private static final int STRIP_Y = 8;

    public static final int STRIP_Y_MIN_OFF  = STRIP_Y;
    public static final int STRIP_Y_MAX_OFF  = 60 - STRIP_H;
    public static final int STRIP_DRAG_RANGE = STRIP_Y_MAX_OFF - STRIP_Y_MIN_OFF;

    // --- кнопка минус ---
    private static final int MINUS_U       = 182;
    private static final int MINUS_V       = 225;
    private static final int MINUS_HOVER_U = 182;
    private static final int MINUS_HOVER_V = 233;
    private static final int MINUS_W       = 11;
    private static final int MINUS_H       = 7;
    public static final  int MINUS_X       = 99;
    public static final  int MINUS_Y       = 13;
    public static final  int MINUS_STEP    = ROW_STEP;

    // --- кнопка плюс ---
    private static final int PLUS_U       = 194;
    private static final int PLUS_V       = 225;
    private static final int PLUS_HOVER_U = 194;
    private static final int PLUS_HOVER_V = 233;
    private static final int PLUS_W       = 11;
    private static final int PLUS_H       = 7;
    public static final  int PLUS_X       = 111;
    public static final  int PLUS_Y       = 13;
    public static final  int PLUS_STEP    = ROW_STEP;

    /** Вычисление текущего XP игрока в "плоских" очках опыта. */
    public static int getClientXp(Player player) {
        int level  = player.experienceLevel;
        float prog = player.experienceProgress;

        int xpToNext;
        if (level >= 30)      xpToNext = 112 + (level - 30) * 9;
        else if (level >= 15) xpToNext = 37  + (level - 15) * 5;
        else                  xpToNext = 7   + level * 2;

        int totalForLevel;
        if (level >= 32)      totalForLevel = (int) (4.5 * level * level - 162.5 * level + 2220);
        else if (level >= 17) totalForLevel = (int) (2.5 * level * level - 40.5  * level + 360);
        else                  totalForLevel = level * level + 6 * level;

        return totalForLevel + (int) (prog * xpToNext);
    }

    /** Кастомный фон инвентаря (вместо ванильного). */
    public static void renderBackground(GuiGraphics gui, int leftPos, int topPos) {
        gui.blit(DAMAGE_CORE_INTERFACE, leftPos, topPos,
                INV_BG_U, INV_BG_V, INV_BG_W, INV_BG_H, ATLAS_SIZE, ATLAS_SIZE);
    }

    // --- доп. фрагмент текстуры U76,V7 -> U94,V79 (18x72), ставится в X76 Y7 инвентаря ---
    // Рисуется ВСЕГДА, в том числе до открытия панели дерева умений (см. вызов в миксине).
    private static final int EXTRA_U = 76;
    private static final int EXTRA_V = 7;
    private static final int EXTRA_W = 18; // 94 - 76
    private static final int EXTRA_H = 72; // 79 - 7
    private static final int EXTRA_X = 76;
    private static final int EXTRA_Y = 7;

    public static void renderExtraPanel(GuiGraphics gui, int leftPos, int topPos) {
        gui.blit(DAMAGE_CORE_INTERFACE,
                leftPos + EXTRA_X, topPos + EXTRA_Y,
                EXTRA_U, EXTRA_V, EXTRA_W, EXTRA_H, ATLAS_SIZE, ATLAS_SIZE);
    }

    /** Полоска скролла списка статов. */
    public static void renderScrollStrip(GuiGraphics gui, int leftPos, int topPos, int stripOffsetY) {
        gui.blit(DAMAGE_CORE_INTERFACE,
                leftPos + STRIP_X, topPos + STRIP_Y_MIN_OFF + stripOffsetY,
                STRIP_U, STRIP_V, STRIP_W, STRIP_H, ATLAS_SIZE, ATLAS_SIZE);
    }

    /**
     * Боковые вкладки (броня / эффекты) с иконками предметов.
     * Используется как в renderBg, так и в render(TAIL) поверх правой панели,
     * поэтому вынесена в один метод, чтобы не дублировать код.
     */
    public static void renderSideTabIcons(GuiGraphics gui, int leftPos, int topPos, int activeSideTab) {
        // Вкладка броня
        int armorU = (activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 208 : 211;
        int armorV = (activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 207 : 179;
        int armorW = (activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 32  : 25;
        int armorX = leftPos + 466 - (activeSideTab == SideTabsRenderer.TAB_ARMOR ? 3 : 0);
        gui.blit(DAMAGE_CORE_INTERFACE,
                armorX, topPos + 4,
                armorU, armorV, armorW, 28, ATLAS_SIZE, ATLAS_SIZE);
        gui.renderItem(new ItemStack(Items.NETHERITE_HELMET),
                armorX + (armorW - 16) / 2, topPos + 4 + 6);

        // Вкладка эффекты
        int potionU = (activeSideTab == SideTabsRenderer.TAB_POTION) ? 240 : 211;
        int potionV = (activeSideTab == SideTabsRenderer.TAB_POTION) ? 207 : 179;
        int potionW = (activeSideTab == SideTabsRenderer.TAB_POTION) ? 32  : 25;
        int potionX = leftPos + 466 - (activeSideTab == SideTabsRenderer.TAB_POTION ? 3 : 0);
        gui.blit(DAMAGE_CORE_INTERFACE,
                potionX, topPos + 33,
                potionU, potionV, potionW, 28, ATLAS_SIZE, ATLAS_SIZE);
        gui.renderItem(new ItemStack(Items.GOLDEN_CARROT),
                potionX + (potionW - 16) / 2, topPos + 33 + 6);
    }

    /**
     * Список строк статов: текст, число, кнопки +/-.
     * Скроллится внутри scissor-региона согласно stripOffsetY.
     */
    public static void renderStatRows(GuiGraphics gui, int leftPos, int topPos,
                                      int mouseX, int mouseY, int stripOffsetY) {
        Player player = Minecraft.getInstance().player;

        Component[] rowLabels = Arrays.stream(StatsType.values())
                .map(s -> Component.translatable(s.getTranslationKey()))
                .toArray(Component[]::new);

        int scrollPx = STRIP_DRAG_RANGE > 0
                ? (stripOffsetY * SCROLL_MAX_PX) / STRIP_DRAG_RANGE : 0;

        gui.enableScissor(leftPos + 97, topPos + 8, leftPos + 163, topPos + 60);
        for (int i = 0; i < ROWS_TOTAL; i++) {
            int rowScreenY = topPos + 8 + i * ROW_STEP - scrollPx;

            StatsType statType = StatsType.values()[i];
            int statValue  = PlayerStatsCapability.get(player).map(s -> s.getStat(statType)).orElse(0);
            int pressCount = PlayerStatsCapability.get(player).map(s -> s.getPressCount(statType)).orElse(0);
            int nextCost   = PlayerStatsCapability.get(player).map(s -> s.getNextCost(statType)).orElse(IPlayerStats.BASE_COST);
            int playerXp   = player != null ? getClientXp(player) : 0;

            boolean isZero      = statValue == 0 && pressCount == 0;
            boolean plusBlocked = pressCount >= IPlayerStats.MAX_LEVEL || playerXp < nextCost;

            // Кнопка минус
            int minusScreenX = leftPos + MINUS_X;
            int minusScreenY = topPos  + MINUS_Y + i * MINUS_STEP - scrollPx;
            boolean minusHovered = !isZero
                    && mouseX >= minusScreenX && mouseX < minusScreenX + MINUS_W / 1.2f
                    && mouseY >= minusScreenY && mouseY < minusScreenY + MINUS_H / 1.2f;
            int minusU = isZero ? 182 : (minusHovered ? MINUS_HOVER_U : MINUS_U);
            int minusV = isZero ? 241 : (minusHovered ? MINUS_HOVER_V : MINUS_V);
            gui.pose().pushPose();
            gui.pose().translate(minusScreenX, minusScreenY, 0);
            gui.pose().scale(1f / 1.2f, 1f / 1.2f, 1f);
            gui.blit(DAMAGE_CORE_INTERFACE, 0, 0, minusU, minusV, MINUS_W, MINUS_H, ATLAS_SIZE, ATLAS_SIZE);
            gui.pose().popPose();

            // Кнопка плюс
            int plusScreenX = leftPos + PLUS_X;
            int plusScreenY = topPos  + PLUS_Y + i * PLUS_STEP - scrollPx;
            boolean plusHovered = !plusBlocked
                    && mouseX >= plusScreenX && mouseX < plusScreenX + PLUS_W / 1.2f
                    && mouseY >= plusScreenY && mouseY < plusScreenY + PLUS_H / 1.2f;
            int plusU = plusBlocked ? 194 : (plusHovered ? PLUS_HOVER_U : PLUS_U);
            int plusV = plusBlocked ? 241 : (plusHovered ? PLUS_HOVER_V : PLUS_V);
            gui.pose().pushPose();
            gui.pose().translate(plusScreenX, plusScreenY, 0);
            gui.pose().scale(1f / 1.2f, 1f / 1.2f, 1f);
            gui.blit(DAMAGE_CORE_INTERFACE, 0, 0, plusU, plusV, PLUS_W, PLUS_H, ATLAS_SIZE, ATLAS_SIZE);
            gui.pose().popPose();

            // Число
            int numX = leftPos + PLUS_X + PLUS_W + 3;
            gui.pose().pushPose();
            gui.pose().translate(numX, plusScreenY, 0);
            gui.pose().scale(1f / 1.4f, 1f / 1.4f, 1f);
            gui.drawString(Minecraft.getInstance().font,
                    Component.literal(String.valueOf(statValue)), 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();

            // Текст строки
            gui.pose().pushPose();
            gui.pose().translate(leftPos + 99, rowScreenY, 0);
            gui.pose().scale(0.5f, 0.5f, 1f);
            gui.drawString(Minecraft.getInstance().font, rowLabels[i], 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();
        }
        gui.disableScissor();
    }

    /** Полный набор: фон + полоска + боковые вкладки + строки статов + игрок (entity) в инвентаре. */
    public static void renderAll(GuiGraphics gui, int leftPos, int topPos,
                                 int mouseX, int mouseY, int stripOffsetY, int activeSideTab) {
        renderBackground(gui, leftPos, topPos);
        renderExtraPanel(gui, leftPos, topPos);
        renderScrollStrip(gui, leftPos, topPos, stripOffsetY);
        renderSideTabIcons(gui, leftPos, topPos, activeSideTab);
        renderStatRows(gui, leftPos, topPos, mouseX, mouseY, stripOffsetY);

        if (Minecraft.getInstance().player != null) {
            int posX = leftPos + 51;
            int posY = topPos + 75;
            int scale = 30; // Масштаб отображения сущности

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui,
                    posX,
                    posY,
                    scale,
                    (int)(posX - mouseX), // Разница по X для слежения взгляда
                    (int)(posY - 50 - mouseY), // Разница по Y для слежения взгляда
                    0.0F, // Дополнительное смещение по X (обычно 0.0F)
                    0.0F, // Дополнительное смещение по Y (обычно 0.0F)
                    0.0F, // Дополнительное смещение по Z (обычно 0.0F)
                    Minecraft.getInstance().player // Сама сущность игрока
            );
        }
    }

}