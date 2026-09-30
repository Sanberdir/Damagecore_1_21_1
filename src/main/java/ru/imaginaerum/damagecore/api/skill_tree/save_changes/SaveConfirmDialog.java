package ru.imaginaerum.damagecore.api.skill_tree.save_changes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
    public static boolean isChanged(StatsType type) {
        return pending.getOrDefault(type, 0) != 0;
    }
    public static boolean canUndo(StatsType type) {
        return pending.getOrDefault(type, 0) > 0;
    }
    public static void markChanged(StatsType type, boolean plus) {
        pending.merge(type, plus ? 1 : -1, Integer::sum);
        if (pending.get(type) == 0) pending.remove(type);
    }
    private static boolean isDirty() { return !pending.isEmpty(); }
    private static void revertChanges() {
        PacketDistributor.sendToServer(new StatSessionPacket(true));
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
                PacketDistributor.sendToServer(new StatSessionPacket(false));
                playClick();
                closeAll(screen);
            } else if (inside(mx, my, x + NO_X, y + BTN_Y, BTN_W, BTN_H)) {
                revertChanges();
                playClick();
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
    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

        float qScale = 0.75f; // размер шрифта вопроса: 1.0 = как раньше, меньше = мельче
        Component question = Component.translatable("damagecore.confirm.save");
        int qCenterX = x + W / 2;
        int qY = y + 14;

        gui.pose().pushPose();
        gui.pose().translate(qCenterX, qY, 0);
        gui.pose().scale(qScale, qScale, 1f);
        gui.drawString(font, question, -font.width(question) / 2, 0, 0xFFFFFF, false);
        gui.pose().popPose();

        drawButton(gui, font, x + YES_X, y + BTN_Y, "damagecore.confirm.yes", mx, my);
        drawButton(gui, font, x + NO_X, y + BTN_Y, "damagecore.confirm.no", mx, my);

        gui.pose().popPose();
    }

    private static final ResourceLocation BUTTON =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private static void drawButton(GuiGraphics gui, Font font, int x, int y, String key, int mx, int my) {
        boolean hovered = inside(mx, my, x, y, BTN_W, BTN_H);

        gui.blitSprite(hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, BTN_W, BTN_H);

        gui.drawCenteredString(font, Component.translatable(key),
                x + BTN_W / 2, y + (BTN_H - font.lineHeight) / 2 + 1, 0xFFFFFF);
    }
}