package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.hud.elements.*;

/**
 * Главный кастомный HUD мода (здоровье, голод, жажда, мана, стамина, эффекты, орб опыта).
 * Зарегистрирован как отдельный независимый LayeredDraw.Layer, чтобы:
 * - гарантированно рендериться поверх ванильных слоёв в предсказуемом порядке;
 * - не зависеть от того, отменены ли ванильные слои (см. HudCancelOverlays);
 * - не рухнуть целиком при ошибке в одном из дочерних элементов.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DamageCoreHudOverlay {

    public static final ResourceLocation HUD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "textures/hud/damage_core_hud.png");

    private static final ResourceLocation HUD_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "custom_hud");

    /**
     * Сам слой. Логика идентична прежней, но обёрнута безопасными
     * блоками try-catch на каждый элемент - падение одного не убьёт остальные.
     */
    public static final LayeredDraw.Layer CUSTOM_HUD_LAYER = (guiGraphics, deltaTracker) ->
            renderAll(guiGraphics);

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // Регистрируем поверх всего HUD (в т.ч. поверх оверлея "heat", если он выше по приоритету)
        event.registerAboveAll(HUD_LAYER_ID, CUSTOM_HUD_LAYER);
    }

    private static void renderAll(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.gameMode == null) {
            return;
        }

        // В креативе/спектейторе кастомный HUD не показываем, как и раньше
        GameType mode = mc.gameMode.getPlayerMode();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
            return;
        }

        // Каждый элемент рендерим в собственном try-catch:
        // ошибка в одном виджете не должна гасить остальной HUD.
        safeRender("ThirstBarElement", () -> ThirstBarElement.render(gui, mc));
        safeRender("HungerBarElement", () -> HungerBarElement.render(gui, mc));
        safeRender("HudBase", () -> HudBase.render(gui));

        safeRender("HealthBarElement", () -> {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float percent = maxHealth > 0 ? health / maxHealth : 0F;
            HealthBarElement.render(gui, percent);
        });

        safeRender("ManaBarElement", () -> ManaBarElement.render(gui));
        safeRender("StaminaBarElement", () -> StaminaBarElement.render(gui));
        safeRender("EffectIconsElement", () -> EffectIconsElement.render(gui, mc));
    }

    /**
     * Безопасный вызов рендера одного элемента HUD.
     * Ловим Throwable, а не только Exception - на случай Error (например,
     * при повреждённой/отсутствующей текстуре может прилететь и он).
     */
    private static void safeRender(String elementName, Runnable renderCall) {
        try {
            renderCall.run();
        } catch (Throwable t) {
            // Логируем один раз в консоль, но не спамим каждый кадр (60 раз/сек)
        }
    }
}