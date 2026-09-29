package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_stats.StatChangePacket;
import ru.imaginaerum.damagecore.library_stats.StatsType;

import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SaveConfirmDialog {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");

    // область окна в текстуре: X310 Y177 - X430 Y237
    private static final int TEX_U = 310, TEX_V = 177, W = 120, H = 60;

    // кнопки относительно левого верхнего угла окна (подстрой под свою текстуру)
    private static final int BTN_W = 45, BTN_H = 15, BTN_Y = 38;
    private static final int YES_X = 9, NO_X = 66;

    private static boolean dirty = false;    // были изменения (нажимали +)
    private static boolean visible = false;  // окно открыто

    public static void markChanged(StatsType type, boolean plus) {
        pending.merge(type, plus ? 1 : -1, Integer::sum);
        if (pending.get(type) == 0) pending.remove(type);
    }
    private static boolean isDirty() { return !pending.isEmpty(); }
    private static void revertChanges() {
        for (Map.Entry<StatsType, Integer> e : pending.entrySet()) {
            int delta = e.getValue();
            boolean undoWithPlus = delta < 0;
            for (int i = 0; i < Math.abs(delta); i++) {
                PacketDistributor.sendToServer(new StatChangePacket(e.getKey(), undoWithPlus));
            }
        }
        pending.clear();
    }
    private static final Map<StatsType, Integer> pending = new EnumMap<>(StatsType.class);
    private static int dx(InventoryScreen s) { return (s.width - W) / 2; }
    private static int dy(InventoryScreen s) { return (s.height - H) / 2; }

    private static void closeAll(InventoryScreen screen) {
        visible = false;
        pending.clear();
        screen.onClose();
    }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------- перехват выхода ----------
    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int key = event.getKeyCode();
        boolean esc = key == GLFW.GLFW_KEY_ESCAPE;
        boolean inv = Minecraft.getInstance().options.keyInventory.matches(key, event.getScanCode());

        if (visible) {
            // Esc внутри окна просто закрывает окно, не сохраняя и не сбрасывая
            if (esc) visible = false;
            event.setCanceled(true);
            return;
        }
        if (isDirty() && (esc || inv)) {
            visible = true;
            event.setCanceled(true);
        }
    }

    // ---------- клики ----------
    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible || !(event.getScreen() instanceof InventoryScreen screen)) return;

        if (event.getButton() == 0) {
            int x = dx(screen), y = dy(screen);
            double mx = event.getMouseX(), my = event.getMouseY();
            if (inside(mx, my, x + YES_X, y + BTN_Y, BTN_W, BTN_H)) {
                closeAll(screen);                 // "Да": изменения уже на сервере
            } else if (inside(mx, my, x + NO_X, y + BTN_Y, BTN_W, BTN_H)) {
                revertChanges();                  // "Нет": откат
                closeAll(screen);
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (visible) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            visible = false;
            dirty = false;
        }
    }

    // ---------- отрисовка ----------
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!visible || !(event.getScreen() instanceof InventoryScreen screen)) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = dx(screen), y = dy(screen);
        int mx = event.getMouseX(), my = event.getMouseY();

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 600); // поверх предметов и тултипов

        gui.fill(0, 0, screen.width, screen.height, 0x88000000);
        gui.blit(TEXTURE, x, y, TEX_U, TEX_V, W, H, 512, 512);

        gui.drawCenteredString(font, Component.translatable("damagecore.confirm.save"),
                x + W / 2, y + 14, 0xFFFFFF);

        drawButton(gui, font, x + YES_X, y + BTN_Y, "damagecore.confirm.yes", mx, my);
        drawButton(gui, font, x + NO_X, y + BTN_Y, "damagecore.confirm.no", mx, my);

        gui.pose().popPose();
    }

    private static void drawButton(GuiGraphics gui, Font font, int x, int y, String key, int mx, int my) {
        if (inside(mx, my, x, y, BTN_W, BTN_H)) {
            gui.fill(x, y, x + BTN_W, y + BTN_H, 0x40FFFFFF);
        }
        gui.drawCenteredString(font, Component.translatable(key),
                x + BTN_W / 2, y + (BTN_H - font.lineHeight) / 2 + 1, 0xFFFFFF);
    }
}