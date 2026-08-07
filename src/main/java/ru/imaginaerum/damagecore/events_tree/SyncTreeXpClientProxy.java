package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.imaginaerum.damagecore.api.skill_tree.DamageBookRenderer;

@OnlyIn(Dist.CLIENT)
public class SyncTreeXpClientProxy {
    public static void apply(SyncTreeXpPacket pkt) {
        try {
            DamageBookRenderer.clearXpData();
            for (var entry : pkt.treeXp.entrySet()) DamageBookRenderer.setXp(entry.getKey(), entry.getValue());
            for (var entry : pkt.treeLevel.entrySet()) DamageBookRenderer.setLevel(entry.getKey(), entry.getValue());

            Object screen = DamageBookRenderer.currentScreen;
            boolean updated = false;
            if (screen != null) {
                try {
                    var m = screen.getClass().getMethod("recalculateProgress");
                    m.invoke(screen);
                    updated = true;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                try {
                    var m2 = screen.getClass().getMethod("updateNodes");
                    m2.invoke(screen);
                    updated = true;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            DamageBookRenderer.forceRefresh();
            if (!updated) {
                try {
                    Screen s = (Screen) DamageBookRenderer.currentScreen;
                    if (s != null) {
                        Minecraft mc = Minecraft.getInstance();
                        mc.setScreen(null);
                        mc.setScreen(s);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}