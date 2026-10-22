package ru.imaginaerum.damagecore.Init.fishing.fight;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public final class FightShake {
    private FightShake() {}

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (true) return;
        Minecraft mc = Minecraft.getInstance();
        if (!FightClient.active || mc.player == null) return;
        float amp = Math.max(0f, FightClient.tension - 0.35f) * 2.2f      // нагрузка на леску
                + (FightClient.dash == 2 ? 0.5f : 0f)                     // рыба рвётся
                + (FightClient.dash == 1 ? 0.25f : 0f);                   // замах
        if (amp <= 0f) return;
        float t = mc.player.tickCount + (float) e.getPartialTick();
        e.setRoll(e.getRoll() + Mth.sin(t * 1.7f) * amp * 0.8f);
        e.setPitch(e.getPitch() + Mth.sin(t * 2.3f + 1f) * amp * 0.4f);
    }
}