package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class ClientHooks {
    public static void openTomeScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int step = mc.player.getData(ModAttachments.TOME_STEP);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        mc.setScreen(new TomeScreen(step));
    }
    public static boolean isReadingMode() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null
                && (mc.player.isShiftKeyDown() || mc.screen instanceof TomeScreen);
    }

    public static boolean isLensMode() {
        var p = Minecraft.getInstance().player;
        return p != null
                && !isReadingMode()
                && p.getData(ModAttachments.TOME_STEP) == 3
                && p.getData(ModAttachments.TOME_LENS_ON);
    }
    public static boolean isSphereMode() {
        var p = Minecraft.getInstance().player;
        return p != null
                && !isReadingMode()
                && p.getData(ModAttachments.TOME_STEP) == TomeLocator.SPHERE_STEP;
    }
    public static boolean isCompassMode() {
        var p = Minecraft.getInstance().player;
        return p != null
                && !isReadingMode()
                && p.getData(ModAttachments.TOME_STEP) == TomeLocator.COMPASS_STEP
                && p.getData(ModAttachments.TOME_COMPASS_ON);
    }
    public static int getTomeStep() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 1 : mc.player.getData(ModAttachments.TOME_STEP);
    }
}