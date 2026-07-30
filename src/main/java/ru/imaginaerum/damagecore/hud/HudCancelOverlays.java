package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT)
public class HudCancelOverlays {

    private static final ResourceLocation ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");

    // Координаты спрайтов пузырьков в icons.png
    private static final int BUBBLE_FULL_U  = 16;
    private static final int BUBBLE_POP_U   = 25;
    private static final int BUBBLE_EMPTY_U = 64;
    private static final int BUBBLE_V       = 18;
    private static final int BUBBLE_SIZE    = 9;
    private static int lastBubbles = -1;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();

        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
        if (name.equals(VanillaGuiLayers.FOOD_LEVEL)) {
            event.setCanceled(true);
        }
        if (name.equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            event.setCanceled(true);
        }
        if (name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            event.setCanceled(true);
        }

        // Отменяем стандартный рендер воздуха и рисуем свой
        if (name.equals(VanillaGuiLayers.AIR_LEVEL)) {
            event.setCanceled(true);
            renderAirBubblesCentered(event.getGuiGraphics());
        }
    }

    private static void renderAirBubblesCentered(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int air = player.getAirSupply();
        int maxAir = player.getMaxAirSupply();
        if (air >= maxAir) {
            lastBubbles = -1;
            return;
        }

        int bubbles = (int) Math.ceil((air - 2) * 10.0 / maxAir);
        int popping = (int) Math.ceil(air * 10.0 / maxAir) - bubbles;

        int totalBubbles = 10;

        // ===== ЗВУК ЛОПАНИЯ =====
        if (lastBubbles != -1 && bubbles < lastBubbles) {
            float pitch = 1.0F + (1.0F - (float) air / maxAir);
            player.playSound(net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.7F, pitch);
        }
        lastBubbles = bubbles;
        // ========================

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int totalWidth = totalBubbles * BUBBLE_SIZE + (totalBubbles - 1);
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 49;

        for (int i = 0; i < totalBubbles; i++) {
            int x = startX + i * (BUBBLE_SIZE + 1);

            int u;
            if (i < bubbles) {
                u = BUBBLE_FULL_U;
            } else if (i < bubbles + popping) {
                u = BUBBLE_POP_U;
            } else {
                u = BUBBLE_EMPTY_U;
            }

            guiGraphics.blit(ICONS, x, y, u, BUBBLE_V, BUBBLE_SIZE, BUBBLE_SIZE, 256, 256);
        }
    }
}