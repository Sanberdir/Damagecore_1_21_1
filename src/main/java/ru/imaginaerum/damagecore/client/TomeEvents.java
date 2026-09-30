package ru.imaginaerum.damagecore.client;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "damagecore")
public class TomeEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int step = player.getData(ModAttachments.TOME_STEP);

        if (step == 1 && player.getData(ModAttachments.TOME_OPENED) && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 2);
        } else if (step == 4 && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 5);
        }

    }
}