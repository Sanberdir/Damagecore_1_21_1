package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectIconsElement {

    private static final int ICON_SIZE = 10;
    private static final int ICON_GAP  = 3;

    private static final int ICON_Y = 4;
    private static final int ICON_START_X = 51;

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        List<MobEffectInstance> list = new ArrayList<>(effects);

        int iconDrawX = ICON_START_X;
        for (MobEffectInstance effectInstance : list) {
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());
            if (sprite != null) {
                gui.blit(iconDrawX, ICON_Y, 0, ICON_SIZE, ICON_SIZE, sprite);
            }
            iconDrawX += ICON_SIZE + ICON_GAP;
        }
    }
}