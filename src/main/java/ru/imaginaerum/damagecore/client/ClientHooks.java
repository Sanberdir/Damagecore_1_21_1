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
    public static int getTomeStep() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 1 : mc.player.getData(ModAttachments.TOME_STEP);
    }
}