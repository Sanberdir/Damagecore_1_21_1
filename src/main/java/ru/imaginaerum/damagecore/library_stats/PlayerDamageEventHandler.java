package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = "damagecore", bus = EventBusSubscriber.Bus.GAME)
public class PlayerDamageEventHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        PlayerStatsCapability.get(player).ifPresent(stats -> {
            int immunityPercent = stats.getImmunityPercent();
            if (immunityPercent <= 0) return;

            float original = event.getAmount();
            float reduced  = original * (1f - immunityPercent / 100f);
            event.setAmount(Math.max(0f, reduced));
        });
    }
}