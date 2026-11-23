package ru.imaginaerum.damagecore.gui_enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.imaginaerum.damagecore.library_enchantments.HigherEnchant;
import ru.imaginaerum.damagecore.library_enchantments.HigherEnchants;
import ru.imaginaerum.damagecore.mixin.enchant_table.AbstractContainerScreenLayoutAccessor;
import ru.imaginaerum.damagecore.mixin.enchant_table.SlotAccessor;

import java.util.ArrayList;
import java.util.List;

public final class EnchantTabs {
    // Область на панели, куда кладём блок
    private static final int AREA_X = 7, AREA_Y = 95;
    private static final int AREA_W = 152, AREA_H = 76;

    // плитки: натуральный размер блоков из png (38x38), 4 колонки x 2 ряда в поле 152x76
    private static final int TILE = 38;
    private static final int TILE_COLS = AREA_W / TILE;   // 4
    private static final int TILE_ROWS = AREA_H / TILE;   // 2
    private static final int TILE_V_EMPTY = 0;       // тёмно-коричневый
    private static final int TILE_V_AVAILABLE = 38;  // бежевый
    private static final int TILE_V_HOVER = 76;      // розовый (под курсором)

    // --- Размеры ванильного окна ---
    private static final int VANILLA_H = 166;
    public static final int TAB_ENCHANT = 0;
    public static final int TAB_STAR    = 1;
    // плитки: натуральный размер блоков из png (38x38), 4 колонки x 2 ряда в поле 152x76
    // --- Текстура второй вкладки ---
    private static final ResourceLocation STAR_TEX = ResourceLocation.fromNamespaceAndPath(
            "damagecore", "textures/gui/container/enchanting_table_2.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 256;
    // --- Статичный ползунок: png (220,0) 6x27 -> панель (163,95) ---
    private static final int SCROLL_U = 220, SCROLL_V = 0, SCROLL_W = 6, SCROLL_H = 27;
    private static final int SCROLL_X = 163, SCROLL_Y = 95;

    // --- Растягиваемый блок: png (176,0) 38x38 ---
    private static final int BLOCK_U = 176, BLOCK_V = 0, BLOCK_W = 38, BLOCK_H = 38;
    private static final int BLOCK_BORDER = 1; // рамка по краям, середина однородная

    // Область на панели, куда кладём блок
    private static final int ROW_INSET = 2, ROW_H = 17, ROW_PITCH = 18, VISIBLE_ROWS = 4;
    private static final int ROW_V_AVAILABLE = 38;   // бежевый блок
    private static final int ROW_V_APPLIED   = 76;   // розовый блок
    // --- Позиции слотов на второй вкладке (координаты ПРЕДМЕТА, не рамки) ---
    private static final int STAR_INV_X     = 8;
    private static final int STAR_INV_Y    = 175; // было 174
    private static final int STAR_HOTBAR_Y = 233; // было 232
    private static final int STAR_ITEM_X    = 80;  // большой слот под предмет (slot 0)
    private static final int STAR_ITEM_Y    = 28;
    private static final int OFFSCREEN      = -3000; // слот лазурита (slot 1) прячем

    // --- Вкладки ---
    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int GAP   = 2;
    private static final int INACTIVE_LIFT = 1;

    private static final ResourceLocation[] SELECTED = {
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"),
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2")
    };
    private static final ResourceLocation[] UNSELECTED = {
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"),
            ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2")
    };
    private static final ItemStack[] ICONS = {
            new ItemStack(Items.ENCHANTING_TABLE),
            new ItemStack(Items.NETHER_STAR)
    };

    private static int active = TAB_ENCHANT;

    private EnchantTabs() {}

    public static int getActive()     { return active; }
    public static boolean isStarTab() { return active == TAB_STAR; }
    public static void reset()        { active = TAB_ENCHANT; }

    private static int tabX(int guiLeft, int i) {
        return guiLeft + i * (TAB_W + GAP);
    }

    private static int tabY(int guiTop, int i) {
        int base = guiTop - TAB_H + 4;
        return active == i ? base : base - INACTIVE_LIFT;
    }

    // ------------------------------------------------------------------
    // Раскладка: высота окна + слоты
    // ------------------------------------------------------------------
    public static void applyLayout(AbstractContainerScreen<?> screen) {
        boolean star = isStarTab();

        int h = star ? PANEL_H : VANILLA_H;
        var acc = (AbstractContainerScreenLayoutAccessor) screen;
        acc.damagecore$setImageHeight(h);
        acc.damagecore$setTopPos((screen.height - h) / 2);

        var slots = screen.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            int x, y;
            if (i == 0) {                       // предмет для зачарования
                x = star ? STAR_ITEM_X : 15;
                y = star ? STAR_ITEM_Y : 47;
            } else if (i == 1) {                // лазурит
                x = star ? OFFSCREEN : 35;
                y = star ? OFFSCREEN : 47;
            } else if (i <= 28) {               // инвентарь 3x9
                int n = i - 2;
                x = 8 + (n % 9) * 18;
                y = (star ? STAR_INV_Y : 84) + (n / 9) * 18;
            } else {                            // хотбар
                int n = i - 29;
                x = 8 + n * 18;
                y = star ? STAR_HOTBAR_Y : 142;
            }
            Slot slot = slots.get(i);
            ((SlotAccessor) slot).damagecore$setX(x);
            ((SlotAccessor) slot).damagecore$setY(y);
        }
    }
    private static int tileAt(int guiLeft, int guiTop, double mx, double my, int count) {
        double dx = mx - guiLeft - AREA_X;
        double dy = my - guiTop - AREA_Y;
        if (dx < 0 || dy < 0 || dx >= TILE_COLS * TILE || dy >= TILE_ROWS * TILE) return -1;

        int i = ((int) dy / TILE) * TILE_COLS + (int) dx / TILE;
        return i < count ? i : -1;
    }
    public static void renderStarPanel(GuiGraphics gui, int x, int y, ItemStack stack,
                                       int mouseX, int mouseY) {
        gui.blit(STAR_TEX, x, y, 0, 0, PANEL_W, PANEL_H, TEX_W, TEX_H);

        // Сплошной фон поля: одна рамка по краю, без перегородок между ячейками
        drawNineSlice(gui, x + AREA_X, y + AREA_Y, AREA_W, AREA_H, BLOCK_V);

        Font font = Minecraft.getInstance().font;
        var player = Minecraft.getInstance().player;
        List<HigherEnchant> list = HigherEnchants.shownFor(stack);
        int hovered = tileAt(x, y, mouseX, mouseY, list.size());

        // Плитки только там, где есть чары
        for (int c = 0; c < list.size() && c < TILE_COLS * TILE_ROWS; c++) {
            int tx = x + AREA_X + (c % TILE_COLS) * TILE;
            int ty = y + AREA_Y + (c / TILE_COLS) * TILE;

            HigherEnchant he = list.get(c);
            boolean applied = HigherEnchants.has(stack, he);

            int v = (c == hovered && !applied) ? TILE_V_HOVER : TILE_V_AVAILABLE;
            gui.blit(STAR_TEX, tx, ty, BLOCK_U, v, TILE, TILE, TEX_W, TEX_H);

            gui.renderItem(new ItemStack(he.getIconItem()), tx + (TILE - 16) / 2, ty + 5);

            Component label;
            int color;
            if (applied) {
                label = Component.translatable("gui.damagecore.higher.applied_short");
                color = 0x55FF55;
            } else {
                boolean enough = player != null
                        && (player.hasInfiniteMaterials() || player.experienceLevel >= he.getLevelCost());
                label = Component.translatable("gui.damagecore.higher.cost_short", he.getLevelCost());
                color = enough ? 0x55FF55 : 0xFF5555;
            }
            gui.drawString(font, label, tx + (TILE - font.width(label)) / 2, ty + 26, color, true);
        }

        if (list.isEmpty()) {
            Component hint = Component.translatable("gui.damagecore.higher.empty");
            List<FormattedCharSequence> lines = font.split(hint, AREA_W - 16);
            int ty = y + AREA_Y + (AREA_H - lines.size() * font.lineHeight) / 2;
            for (FormattedCharSequence line : lines) {
                gui.drawString(font, line, x + AREA_X + (AREA_W - font.width(line)) / 2, ty, 0xC0C0C0, true);
                ty += font.lineHeight;
            }
        }

        gui.blit(STAR_TEX, x + SCROLL_X, y + SCROLL_Y,
                SCROLL_U, SCROLL_V, SCROLL_W, SCROLL_H, TEX_W, TEX_H);
    }
    private static int rowAt(AbstractContainerScreen<?> screen, double mx, double my, int count) {
        int left = screen.getGuiLeft() + AREA_X + ROW_INSET;
        int top = screen.getGuiTop() + AREA_Y + ROW_INSET;
        int w = AREA_W - 2 * ROW_INSET;
        if (mx < left || mx >= left + w || my < top) return -1;

        int i = (int) ((my - top) / ROW_PITCH);
        if (i < 0 || i >= Math.min(count, VISIBLE_ROWS)) return -1;
        if ((my - top) - i * ROW_PITCH >= ROW_H) return -1;
        return i;
    }

    /** Вызывать в конце render экрана, после renderTabs. */
    public static void renderHover(GuiGraphics gui, AbstractContainerScreen<?> screen, int mx, int my) {
        if (!isStarTab()) return;
        ItemStack stack = screen.getMenu().getSlot(0).getItem();
        List<HigherEnchant> list = HigherEnchants.shownFor(stack);
        int i = tileAt(screen.getGuiLeft(), screen.getGuiTop(), mx, my, list.size());
        if (i < 0) return;

        HigherEnchant he = list.get(i);
        List<Component> lines = new ArrayList<>();
        lines.add(he.getDisplayName());
        lines.add(he.getDescription().withStyle(ChatFormatting.GRAY));
        lines.add(HigherEnchants.has(stack, he)
                ? Component.translatable("gui.damagecore.higher.applied").withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.damagecore.higher.cost", he.getLevelCost())
                .withStyle(ChatFormatting.YELLOW));
        gui.renderComponentTooltip(Minecraft.getInstance().font, lines, mx, my);
    }
    /** Растягивает блок BLOCK_* на w*h: углы и рамка 1 px не масштабируются, середина тянется. */
    private static void drawNineSlice(GuiGraphics gui, int x, int y, int w, int h, int blockV) {
        int b = BLOCK_BORDER;
        int cw = BLOCK_W - 2 * b;
        int ch = BLOCK_H - 2 * b;
        int mw = w - 2 * b;
        int mh = h - 2 * b;

        int u0 = BLOCK_U, u1 = BLOCK_U + b, u2 = BLOCK_U + BLOCK_W - b;
        int v0 = blockV, v1 = blockV + b, v2 = blockV + BLOCK_H - b;
        // Углы
        gui.blit(STAR_TEX, x,         y,         b, b, u0, v0, b, b, TEX_W, TEX_H);
        gui.blit(STAR_TEX, x + w - b, y,         b, b, u2, v0, b, b, TEX_W, TEX_H);
        gui.blit(STAR_TEX, x,         y + h - b, b, b, u0, v2, b, b, TEX_W, TEX_H);
        gui.blit(STAR_TEX, x + w - b, y + h - b, b, b, u2, v2, b, b, TEX_W, TEX_H);

        // Верх и низ
        gui.blit(STAR_TEX, x + b, y,         mw, b, u1, v0, cw, b, TEX_W, TEX_H);
        gui.blit(STAR_TEX, x + b, y + h - b, mw, b, u1, v2, cw, b, TEX_W, TEX_H);

        // Лево и право
        gui.blit(STAR_TEX, x,         y + b, b, mh, u0, v1, b, ch, TEX_W, TEX_H);
        gui.blit(STAR_TEX, x + w - b, y + b, b, mh, u2, v1, b, ch, TEX_W, TEX_H);

        // Центр
        gui.blit(STAR_TEX, x + b, y + b, mw, mh, u1, v1, cw, ch, TEX_W, TEX_H);
    }

    /** Вызывать в конце render экрана: рисует только вкладки. */
    public static void renderTabs(GuiGraphics gui, int guiLeft, int guiTop) {
        for (int i = 0; i < ICONS.length; i++) {
            int x = tabX(guiLeft, i);
            int y = tabY(guiTop, i);
            ResourceLocation sprite = (active == i) ? SELECTED[i] : UNSELECTED[i];
            gui.blitSprite(sprite, x, y, TAB_W, TAB_H);
            gui.renderItem(ICONS[i], x + 5, y + 8);
        }
    }

    // ------------------------------------------------------------------
    // Клики
    // ------------------------------------------------------------------
    /** true, если клик надо поглотить. */
    public static boolean mouseClicked(double mx, double my, int button,
                                       AbstractContainerScreen<?> screen) {
        if (button != 0) return false;
        int guiLeft = screen.getGuiLeft();
        int guiTop  = screen.getGuiTop();

        for (int i = 0; i < ICONS.length; i++) {
            int x = tabX(guiLeft, i);
            int y = tabY(guiTop, i);
            if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) {
                if (active != i) {
                    active = i;
                    applyLayout(screen);
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        if (isStarTab()) {
            ItemStack stack = screen.getMenu().getSlot(0).getItem();
            List<HigherEnchant> list = HigherEnchants.shownFor(stack);
            int i = tileAt(guiLeft, guiTop, mx, my, list.size());
            if (i >= 0) {
                HigherEnchant he = list.get(i);
                var mc = Minecraft.getInstance();
                if (HigherEnchants.canApply(mc.player, stack, he)) {
                    mc.gameMode.handleInventoryButtonClick(
                            screen.getMenu().containerId, HigherEnchants.BUTTON_BASE + he.ordinal());
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return false;
    }
}