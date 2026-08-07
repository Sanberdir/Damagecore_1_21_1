package ru.imaginaerum.damagecore.library_stats.attributes;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@EventBusSubscriber(modid = "damagecore")
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyStats(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyStats(event.getEntity());
    }

    private static void applyStats(Player player) {
        PlayerStatsCapability.get(player).ifPresent(stats -> {
            int liveForgeLevel = stats.getStat(StatsType.LIVE_FORCE);
            AttributeApplier.applyLiveForge(player, liveForgeLevel);

            // Обрезаем текущее HP если оно выше нового максимума
            float newMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        });
    }
}