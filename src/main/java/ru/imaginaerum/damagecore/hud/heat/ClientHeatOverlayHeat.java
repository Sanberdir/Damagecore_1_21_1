package ru.imaginaerum.damagecore.hud.heat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

/**
 * Оверлей "Heat" - полупрозрачная текстура, появляющаяся на экране
 * по мере накопления жара, по аналогии с ванильным оверлеем обморожения.
 *
 * В NeoForge 1.21.1 старый IGuiOverlay/RegisterGuiOverlaysEvent удалены,
 * вместо них используется ванильный интерфейс LayeredDraw.Layer
 * и событие RegisterGuiLayersEvent.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientHeatOverlayHeat {

    // Текстура оверлея: assets/damagecore/textures/hud/heat.png
    private static final ResourceLocation HEAT_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "textures/hud/heat.png");

    // Уникальный идентификатор слоя (нужен для регистрации и упорядочивания)
    private static final ResourceLocation HEAT_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "heat_overlay");

    /**
     * Реализация слоя HUD. Сигнатура render теперь принимает только
     * GuiGraphics и DeltaTracker - ширина/высота берутся из guiGraphics.
     */
    public static final LayeredDraw.Layer HEAT_OVERLAY = (guiGraphics, deltaTracker) -> {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int heat = player.getData(ModAttachmentsHeat.HEAT.get());
        if (heat <= 0) {
            return; // жара нет - оверлей не рисуем
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // Прозрачность оверлея растёт пропорционально накопленному жару
        float alpha = Math.min(1.0F, (float) heat / HeatEventHandler.MAX_HEAT);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        guiGraphics.blit(HEAT_OVERLAY_TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    };

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // Регистрируем слой поверх всех остальных элементов HUD
        event.registerAboveAll(HEAT_LAYER_ID, HEAT_OVERLAY);
    }
}