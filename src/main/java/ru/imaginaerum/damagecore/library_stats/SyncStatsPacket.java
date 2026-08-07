package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class SyncStatsPacket implements CustomPacketPayload {

    // 1. Регистрируем уникальный идентификатор пакета
    public static final CustomPacketPayload.Type<SyncStatsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sync_stats"));

    // 2. Создаем потоковый кодек вместо старых encode/decode
    public static final StreamCodec<FriendlyByteBuf, SyncStatsPacket> CODEC = StreamCodec.of(
            SyncStatsPacket::encode,
            SyncStatsPacket::decode
    );

    final int[] statValues;
    final int[] pressCounts;
    final int   totalXp;

    // ИСПРАВЛЕНО: Изменен тип аргумента с IPlayerStats на PlayerStats для точного совпадения типов
    public SyncStatsPacket(PlayerStats stats, int totalXp) {
        StatsType[] types = StatsType.values();
        this.statValues  = new int[types.length];
        this.pressCounts = new int[types.length];
        this.totalXp     = totalXp;
        for (int i = 0; i < types.length; i++) {
            this.statValues[i]  = stats.getStat(types[i]);
            this.pressCounts[i] = stats.getPressCount(types[i]);
        }
    }

    private SyncStatsPacket(int[] statValues, int[] pressCounts, int totalXp) {
        this.statValues  = statValues;
        this.pressCounts = pressCounts;
        this.totalXp     = totalXp;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, SyncStatsPacket packet) {
        buf.writeVarInt(packet.statValues.length);
        for (int v : packet.statValues)  buf.writeVarInt(v);
        for (int c : packet.pressCounts) buf.writeVarInt(c);
        buf.writeVarInt(packet.totalXp);
    }

    private static SyncStatsPacket decode(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        int[] statValues  = new int[len];
        int[] pressCounts = new int[len];
        for (int i = 0; i < len; i++) statValues[i]  = buf.readVarInt();
        for (int i = 0; i < len; i++) pressCounts[i] = buf.readVarInt();
        int totalXp = buf.readVarInt();
        return new SyncStatsPacket(statValues, pressCounts, totalXp);
    }

    public static void handle(final SyncStatsPacket packet, final IPayloadContext ctx) {
        // Проверяем, что пакет пришёл именно на сторону клиента
        if (ctx.flow().isClientbound()) {
            SyncStatsClientProxy.apply(packet);
        }
    }
}
