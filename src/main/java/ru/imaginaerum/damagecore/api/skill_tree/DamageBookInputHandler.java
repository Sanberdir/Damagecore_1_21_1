package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class DamageBookInputHandler {
    private DamageBookInputHandler() {}

    public static int handleSmallTabsClick(double mouseX, double mouseY, InventoryScreen screen,
                                           int oldSelectedSmall, int tabX, int tabY) {
        int[] smallYOffsets = {3, 3 + 26 + 1};
        Minecraft mc = Minecraft.getInstance();

        for (int i = 0; i < 2; i++) {
            int smallX1 = tabX - 29;
            int smallY1 = tabY + smallYOffsets[i];
            int smallX2 = smallX1 + 30;
            int smallY2 = smallY1 + 26;

            if (mouseX >= smallX1 && mouseX < smallX2 && mouseY >= smallY1 && mouseY < smallY2) {
                if (oldSelectedSmall != i) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return i;
                }
                break;
            }
        }
        return oldSelectedSmall;
    }
}
