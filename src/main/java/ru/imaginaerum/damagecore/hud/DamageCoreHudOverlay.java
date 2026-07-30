package ru.imaginaerum.damagecore.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber; // Исправленный импорт
import net.neoforged.neoforge.client.event.RenderGuiEvent;

// Добавленные импорты для всех элементов HUD
import ru.imaginaerum.damagecore.hud.elements.HudBase;
import ru.imaginaerum.damagecore.hud.elements.HungerBarElement;
import ru.imaginaerum.damagecore.hud.elements.ThirstBarElement;
import ru.imaginaerum.damagecore.hud.elements.HealthBarElement;
import ru.imaginaerum.damagecore.hud.elements.ManaBarElement;
import ru.imaginaerum.damagecore.hud.elements.StaminaBarElement;
import ru.imaginaerum.damagecore.hud.elements.EffectIconsElement;

// Исправлено: убрали "Mod." и указали шину FORGE (по умолчанию), так как RenderGuiEvent вызывается на игровой шине
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class DamageCoreHudOverlay {

    // Исправлено: в 1.21.1 используется ResourceLocation.fromNamespaceAndPath
    public static final ResourceLocation HUD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/hud/damage_core_hud.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) return;

        GameType mode = mc.gameMode.getPlayerMode();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return;

        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        ThirstBarElement.tick(mc);
        ThirstBarElement.render(gui, mc);
        HungerBarElement.render(gui, mc);
        HudBase.render(gui);

        LocalPlayer player = mc.player;
        if (player != null) {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float percent = health / maxHealth;
            HealthBarElement.render(gui, percent);
        }
        ManaBarElement.render(gui);
        StaminaBarElement.render(gui);
        EffectIconsElement.render(gui, mc);
    }
}
