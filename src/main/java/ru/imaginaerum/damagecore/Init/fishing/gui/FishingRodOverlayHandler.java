package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class FishingRodOverlayHandler {

    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");
    private static final int SLOT = 18;
    private static final int GRID = SLOT * 2;
    private static final int PADDING = 4;
    private static final int BRIDGE_PAD = 20;

    private static int activeRodSlot = -1;
    private static int gridLeft = -1;
    private static int gridTop = -1;

    private static double lastMouseX = -1, lastMouseY = -1;
    private static int tipRodSlot = -1;

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        tipRodSlot = -1;
        lastMouseX = event.getMouseX();
        lastMouseY = event.getMouseY();
    }

    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (activeRodSlot >= 0 && isOverGrid(lastMouseX, lastMouseY)) {
            event.setCanceled(true);
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FISHING_ROD)) return;
        if (!(mc.screen instanceof InventoryScreen invScreen)) return;
        if (mc.player == null) return;

        Slot slot = invScreen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem() || !slot.getItem().is(Items.FISHING_ROD)) return;
        if (slot.container != mc.player.getInventory()) return;

        tipRodSlot = slot.getContainerSlot();
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen invScreen)) {
            deactivate();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            deactivate();
            return;
        }

        double mx = event.getMouseX();
        double my = event.getMouseY();

        boolean overCurrentGrid = isOverGrid(mx, my);
        Slot hovered = invScreen.getSlotUnderMouse();
        boolean hoveringRod = hovered != null && hovered.hasItem()
                && hovered.getItem().is(Items.FISHING_ROD)
                && hovered.container == mc.player.getInventory();

        // ИСПРАВЛЕНИЕ: Если оверлей уже активен, НЕ переключаем его на другую удочку,
        // пока мышь находится над текущим оверлеем или в зоне удержания!
        if (activeRodSlot < 0) {
            if (hoveringRod) {
                activeRodSlot = hovered.getContainerSlot();
                positionGridNearSlot(invScreen, hovered);
            }
        } else {
            // Если оверлей уже открыт, проверяем, не вышел ли курсор за пределы рабочей зоны
            Slot currentRodSlot = findRodSlot(invScreen, mc, activeRodSlot);
            if (currentRodSlot != null && !inKeepAlive(invScreen, currentRodSlot, mx, my) && !isOverGrid(mx, my)) {
                deactivate(); // Закрываем только тогда, когда реально увели мышь далеко
            }
        }
        if (hoveringRod && tipRodSlot == hovered.getContainerSlot()) {
            activeRodSlot = hovered.getContainerSlot();
            positionGridNearSlot(invScreen, hovered);
        } else if (activeRodSlot < 0 && hoveringRod) {
            // Фолбэк на случай, если tipRodSlot не успел обновиться
            activeRodSlot = hovered.getContainerSlot();
            positionGridNearSlot(invScreen, invScreen.getMenu().getSlot(activeRodSlot));
        }

        if (activeRodSlot < 0 || gridLeft < 0) return;

        Slot rodSlot = findRodSlot(invScreen, mc, activeRodSlot);
        ItemStack rod = mc.player.getInventory().getItem(activeRodSlot);
        if (rodSlot == null || rod.isEmpty() || !rod.is(Items.FISHING_ROD)) {
            deactivate();
            return;
        }

        // Проверяем, что мышь всё еще либо около ванильного слота, либо над самим оверлеем
        if (!inKeepAlive(invScreen, rodSlot, mx, my) && !overCurrentGrid) {
            deactivate();
            return;
        }

        GuiGraphics g = event.getGuiGraphics();

        int panelX = gridLeft - 1 - PADDING;
        int panelY = gridTop - 1 - PADDING;
        int panelW = GRID + PADDING * 2;
        int panelH = GRID + PADDING * 2;

        ItemContainerContents contents = rod.getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // поверх слотов, предметов и подсветки ванили

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF100010);
        g.renderOutline(panelX, panelY, panelW, panelH, 0xFF5000FF);
        g.renderOutline(panelX + 1, panelY + 1, panelW - 2, panelH - 2, 0xFF28007F);

        g.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int idx = row * 2 + col;
                int sx = gridLeft + col * SLOT;
                int sy = gridTop + row * SLOT;

                g.blitSprite(SLOT_SPRITE, sx - 1, sy - 1, 18, 18);

                boolean isHovered = mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16;
                if (isHovered) {
                    ItemStack carried = invScreen.getMenu().getCarried();
                    boolean bad = !carried.isEmpty() && !isValidForSlot(carried, idx);
                    g.fill(sx, sy, sx + 16, sy + 16, bad ? 0x80FF0000 : 0x80FFFFFF);
                }

                boolean filled = idx < contents.getSlots() && !contents.getStackInSlot(idx).isEmpty();
                if (filled) {
                    ItemStack s = contents.getStackInSlot(idx);
                    g.renderItem(s, sx, sy);
                    g.renderItemDecorations(mc.font, s, sx, sy);
                } else {
                    ItemStack hint = getHintStack(idx);
                    if (!hint.isEmpty()) {
                        g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                        g.renderItem(hint, sx, sy);
                    }
                }
            }
        }

        ItemStack carried = invScreen.getMenu().getCarried();
        if (!carried.isEmpty() && isOverGrid(mx, my)) {
            g.renderItem(carried, (int) mx - 8, (int) my - 8);
            g.renderItemDecorations(mc.font, carried, (int) mx - 8, (int) my - 8);
        }

        g.disableScissor();
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (activeRodSlot >= 0 && isOverGrid(event.getMouseX(), event.getMouseY())) {
            int col = ((int) event.getMouseX() - gridLeft) / SLOT;
            int row = ((int) event.getMouseY() - gridTop) / SLOT;
            int idx = row * 2 + col;

            if (idx >= 0 && idx < 4) {
                PacketDistributor.sendToServer(new RodSlotClickPacket(
                        activeRodSlot, idx, event.getButton(), Screen.hasShiftDown()));
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (activeRodSlot >= 0 && isOverGrid(event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    // ===== Хелперы =====

    private static boolean isOverGrid(double mx, double my) {
        return activeRodSlot >= 0 && gridLeft >= 0
                && mx >= gridLeft && mx < gridLeft + GRID
                && my >= gridTop && my < gridTop + GRID;
    }

    private static Slot findRodSlot(InventoryScreen invScreen, Minecraft mc, int containerSlot) {
        for (Slot s : invScreen.getMenu().slots) {
            if (s.container == mc.player.getInventory() && s.getContainerSlot() == containerSlot) {
                return s;
            }
        }
        return null;
    }

    private static boolean inKeepAlive(InventoryScreen invScreen, Slot rodSlot, double mx, double my) {
        int rx = invScreen.getGuiLeft() + rodSlot.x;
        int ry = invScreen.getGuiTop() + rodSlot.y;
        int gx = gridLeft - 1 - PADDING;
        int gy = gridTop - 1 - PADDING;
        int gw = GRID + PADDING * 2;
        int gh = GRID + PADDING * 2;

        int x1 = Math.min(rx, gx) - BRIDGE_PAD;
        int y1 = Math.min(ry, gy) - BRIDGE_PAD;
        int x2 = Math.max(rx + 16, gx + gw) + BRIDGE_PAD;
        int y2 = Math.max(ry + 16, gy + gh) + BRIDGE_PAD;

        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    private static void positionGridNearSlot(InventoryScreen invScreen, Slot rodSlot) {
        Minecraft mc = Minecraft.getInstance();
        int panelW = GRID + PADDING * 2;
        int panelH = GRID + PADDING * 2;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int slotScreenX = invScreen.getGuiLeft() + rodSlot.x;
        int slotScreenY = invScreen.getGuiTop() + rodSlot.y;

        int panelX = slotScreenX - 1;
        if (panelX + panelW > screenW) panelX = screenW - panelW;
        if (panelX < 0) panelX = 0;

        int panelY = slotScreenY + 17 + 2;

        if (panelY + panelH > screenH) {
            panelY = slotScreenY - 1 - panelH - 2;
        }

        if (panelY < 0) panelY = 0;

        gridLeft = panelX + 1 + PADDING;
        gridTop = panelY + 1 + PADDING;
    }

    private static void deactivate() {
        activeRodSlot = -1;
        gridLeft = -1;
        gridTop = -1;
    }

    private static boolean isValidForSlot(ItemStack stack, int idx) {
        return switch (idx) {
            case 0 -> stack.is(ModItemTags.LINES);
            case 1 -> stack.is(ModItemTags.HOOKS);
            case 2 -> stack.is(ModItemTags.BAITS);
            case 3 -> stack.is(ModItemTags.REELS);
            default -> false;
        };
    }

    private static ItemStack getHintStack(int idx) {
        TagKey<Item> tag = switch (idx) {
            case 0 -> ModItemTags.LINES;
            case 1 -> ModItemTags.HOOKS;
            case 2 -> ModItemTags.BAITS;
            case 3 -> ModItemTags.REELS;
            default -> null;
        };
        if (tag == null) return ItemStack.EMPTY;

        var holders = BuiltInRegistries.ITEM.getTag(tag);
        if (holders.isPresent() && holders.get().size() > 0) {
            Item firstItem = holders.get().get(0).value();
            return new ItemStack(firstItem);
        }
        return ItemStack.EMPTY;
    }
}