package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT)
public class HudCancelOverlays {

    // Спрайты, которые точно есть в атласе GUI
    private static final ResourceLocation AIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/air");

    private static final int BUBBLE_SIZE = 9;
    private static final int TOTAL_BUBBLES = 10;

    // Для отслеживания звука лопания
    private static int lastBubbles = -1;

    // Таймеры анимации для каждого пузырька (в тиках)
    private static final int[] popTimers = new int[TOTAL_BUBBLES];
    private static final int POP_ANIMATION_TICKS = 6; // длительность анимации лопания

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();

        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
                || name.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            event.setCanceled(true);
        }

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

        // Воздух полный — сбрасываем всё и ничего не рисуем
        if (air >= maxAir) {
            lastBubbles = -1;
            for (int i = 0; i < TOTAL_BUBBLES; i++) popTimers[i] = 0;
            return;
        }

        int bubbles = (int) Math.ceil((air - 2) * 10.0 / maxAir);
        int popping = (int) Math.ceil(air * 10.0 / maxAir) - bubbles;

        // 🔊 Звук лопания: если пузырьков стало меньше, чем в прошлом кадре
        if (lastBubbles != -1 && bubbles < lastBubbles) {
            float pitch = 1.0F + (1.0F - (float) air / maxAir);
            player.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.7F, pitch);
            // Если вы на 1.21.2+, можно использовать ui.hud.bubble_pop:
            // player.playSound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
            //         .getHolder(ResourceLocation.withDefaultNamespace("ui.hud.bubble_pop"))
            //         .map(holder -> holder.value()).orElse(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP),
            //         0.7F, pitch);

            // Запускаем анимацию для только что лопнувших пузырьков
            int newlyPopped = lastBubbles - bubbles;
            int startIndex = Math.max(0, bubbles - popping); // индекс первого лопнувшего в этом кадре
            // Проще: помечаем все лопнувшие пузырьки (индексы от bubbles до bubbles+popping-1)
            for (int i = bubbles; i < bubbles + popping && i < TOTAL_BUBBLES; i++) {
                popTimers[i] = POP_ANIMATION_TICKS;
            }
        }
        lastBubbles = bubbles;

        // Уменьшаем таймеры анимации (каждый кадр, не только при лопании)
        for (int i = 0; i < TOTAL_BUBBLES; i++) {
            if (popTimers[i] > 0) popTimers[i]--;
        }

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int totalWidth = TOTAL_BUBBLES * BUBBLE_SIZE + (TOTAL_BUBBLES - 1);
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 49;

        int visible = Math.min(bubbles + popping, TOTAL_BUBBLES);

        for (int i = 0; i < visible; i++) {
            int x = startX + i * (BUBBLE_SIZE + 1);

            if (i < bubbles) {
                // Полный пузырёк
                guiGraphics.blitSprite(AIR_SPRITE, x, y, BUBBLE_SIZE, BUBBLE_SIZE);
            } else {
                // Лопнувший пузырёк — анимация
                if (popTimers[i] > 0) {
                    float progress = 1.0F - (popTimers[i] / (float) POP_ANIMATION_TICKS); // 0.0 → 1.0
                    float alpha = 1.0F - progress; // исчезает
                    float scale = 1.0F + progress * 0.6F; // немного увеличивается

                    int size = (int) (BUBBLE_SIZE * scale);
                    int offset = (size - BUBBLE_SIZE) / 2;

                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                    guiGraphics.blitSprite(AIR_SPRITE, x - offset, y - offset, size, size);
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    // Таймер истёк — просто полупрозрачный «остаток» (или можно не рисовать)
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.4F);
                    guiGraphics.blitSprite(AIR_SPRITE, x, y, BUBBLE_SIZE, BUBBLE_SIZE);
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }
}