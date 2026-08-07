package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SyncStatsClientProxy {
    public static void apply(SyncStatsPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerStatsCapability.get(player).ifPresent(stats -> {
            StatsType[] types = StatsType.values();
            for (int i = 0; i < types.length && i < packet.statValues.length; i++) {
                stats.setStat(types[i], packet.statValues[i]);
                stats.setPressCount(types[i], packet.pressCounts[i]);
            }
        });
    }
}