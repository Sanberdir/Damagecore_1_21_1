package ru.imaginaerum.damagecore.api.skill_tree.save_changes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_stats.*;
import ru.imaginaerum.damagecore.library_stats.attributes.AttributeApplier;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record StatSessionPacket(boolean revert) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatSessionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "stat_session"));

    public static final StreamCodec<FriendlyByteBuf, StatSessionPacket> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBoolean(p.revert),
            buf -> new StatSessionPacket(buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    // ---------- снимки ----------
    private record Snapshot(Map<StatsType, Integer> stats, Map<StatsType, Integer> press, int totalXp) {}
    private static final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<StatsType, Integer>> session = new ConcurrentHashMap<>();

    public static int getSession(ServerPlayer player, StatsType type) {
        Map<StatsType, Integer> m = session.get(player.getUUID());
        return m == null ? 0 : m.getOrDefault(type, 0);
    }

    public static void addSession(ServerPlayer player, StatsType type, int delta) {
        session.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(StatsType.class))
                .merge(type, delta, Integer::sum);
    }
    /** Вызывать в StatChangePacket.handle ПЕРЕД изменением, после получения stats */
    public static void ensureSnapshot(ServerPlayer player, PlayerStats stats) {

        snapshots.computeIfAbsent(player.getUUID(), id -> {
            Map<StatsType, Integer> s = new EnumMap<>(StatsType.class);
            Map<StatsType, Integer> p = new EnumMap<>(StatsType.class);
            for (StatsType t : StatsType.values()) {
                s.put(t, stats.getStat(t));
                p.put(t, stats.getPressCount(t));
            }
            return new Snapshot(s, p, xpOf(player));
        });
    }

    public static void handle(StatSessionPacket packet, IPayloadContext ctx) {
        if (!ctx.flow().isServerbound()) return;
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player == null) return;

        Snapshot snap = snapshots.remove(player.getUUID());
        session.remove(player.getUUID()); // и "Да", и "Нет" завершают сессию

        // "Да": ничего не трогаем, pressCount и цены остаются как есть
        if (!packet.revert()) return;

        // "Нет": откат
        if (snap == null) return;

        var opt = PlayerStatsCapability.get(player);
        if (opt.isEmpty()) return;
        PlayerStats stats = opt.get();

        for (StatsType t : StatsType.values()) {
            stats.setStat(t, snap.stats().get(t));
            stats.setPressCount(t, snap.press().get(t));
        }

        player.setExperienceLevels(0);
        player.experienceProgress = 0f;
        player.totalExperience = 0;
        player.giveExperiencePoints(snap.totalXp());

        AttributeApplier.applyLiveForge(player, stats.getStat(StatsType.LIVE_FORCE));
        float newMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        if (player.getHealth() > newMax) player.setHealth(newMax);

        PacketDistributor.sendToPlayer(player, new SyncStatsPacket(stats, xpOf(player)));
    }

    public static void clear(UUID id) { snapshots.remove(id); session.remove(id); }

    private static int xpOf(ServerPlayer player) {
        return StatChangePacket.getServerXp(player);
    }
}