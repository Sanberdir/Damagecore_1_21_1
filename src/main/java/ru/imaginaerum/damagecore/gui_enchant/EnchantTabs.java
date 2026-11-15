package ru.imaginaerum.damagecore.gui_enchant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EnchantTabs {

    public static final int TAB_ENCHANT = 0;
    public static final int TAB_STAR    = 1;

    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int GAP   = 2;
    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 166;

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

    private static int tabX(int guiLeft, int i) { return guiLeft + i * (TAB_W + GAP); }
    private static int tabY(int guiTop)          { return guiTop - TAB_H + 4; }

    /** Вызывать в конце render экрана. */
    public static void render(GuiGraphics gui, int guiLeft, int guiTop, int mouseX, int mouseY) {
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 400); // поверх предметов и слотов

        // Панель второй вкладки перекрывает ванильное содержимое
        if (isStarTab()) {
            renderStarPanel(gui, guiLeft, guiTop, mouseX, mouseY);
        }

        for (int i = 0; i < ICONS.length; i++) {
            boolean on = active == i;
            int x = tabX(guiLeft, i);
            int y = tabY(guiTop) - (on ? 0 : 1); // неактивная выше на 2 px
            ResourceLocation sprite = on ? SELECTED[i] : UNSELECTED[i];
            gui.blitSprite(sprite, x, y, TAB_W, TAB_H);
            gui.renderItem(ICONS[i], x + 5, y + 8);
        }

        gui.pose().popPose();
    }

    private static void renderStarPanel(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        // Простой серый фон с рамкой, замените на свою текстуру
        gui.fill(x, y, x + IMAGE_W, y + IMAGE_H, 0xFF000000);
        gui.fill(x + 1, y + 1, x + IMAGE_W - 1, y + IMAGE_H - 1, 0xFFFFFFFF);
        gui.fill(x + 3, y + 3, x + IMAGE_W - 3, y + IMAGE_H - 3, 0xFFC6C6C6);

        gui.drawString(Minecraft.getInstance().font, "Star", x + 8, y + 8, 0xFF404040, false);
        // TODO: содержимое второй вкладки
    }

    /** true, если клик надо поглотить. */
    public static boolean mouseClicked(double mx, double my, int button, int guiLeft, int guiTop) {
        if (button != 0) return false;

        for (int i = 0; i < ICONS.length; i++) {
            int x = tabX(guiLeft, i);
            int y = tabY(guiTop);
            if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) {
                if (active != i) {
                    active = i;
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        // Пока открыта звезда, клики внутри панели не должны попадать в слоты и кнопки чар под ней
        if (isStarTab()
                && mx >= guiLeft && mx < guiLeft + IMAGE_W
                && my >= guiTop  && my < guiTop + IMAGE_H) {
            // TODO: клики по содержимому второй вкладки
            return true;
        }
        return false;
    }
}