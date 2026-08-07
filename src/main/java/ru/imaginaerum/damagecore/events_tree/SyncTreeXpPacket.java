package ru.imaginaerum.damagecore.events_tree;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.HashMap;
import java.util.Map;

public class SyncTreeXpPacket implements CustomPacketPayload {

    // 1.21.1: каждый пакет должен иметь уникальный Type<...> с id — используется вместо старой регистрации по имени в SimpleChannel.
    public static final Type<SyncTreeXpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "sync_tree_xp"));

    // 1.21.1: StreamCodec заменяет пару статических методов encode(...)/decode(...) из старого API.
    // ByteBuf годится, т.к. пакету не нужны registry-aware типы (ItemStack, Holder<...> и т.п.) — только int'ы.
    public static final StreamCodec<ByteBuf, SyncTreeXpPacket> STREAM_CODEC = StreamCodec.of(
            SyncTreeXpPacket::encode,
            SyncTreeXpPacket::decode
    );

    public final Map<Integer, Integer> treeXp;
    public final Map<Integer, Integer> treeLevel;

    public SyncTreeXpPacket(Map<Integer, Integer> treeXp, Map<Integer, Integer> treeLevel) {
        this.treeXp = treeXp != null ? new HashMap<>(treeXp) : new HashMap<>();
        this.treeLevel = treeLevel != null ? new HashMap<>(treeLevel) : new HashMap<>();
    }

    public SyncTreeXpPacket(int singleTreeId, int singleXp) {
        Map<Integer, Integer> xp = new HashMap<>();
        xp.put(singleTreeId, singleXp);
        this.treeXp = xp;
        this.treeLevel = new HashMap<>();
    }

    public static SyncTreeXpPacket single(int treeId, int xp) {
        return new SyncTreeXpPacket(treeId, xp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, SyncTreeXpPacket pkt) {
        buf.writeInt(pkt.treeXp.size());
        for (var entry : pkt.treeXp.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeInt(pkt.treeLevel.size());
        for (var entry : pkt.treeLevel.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    private static SyncTreeXpPacket decode(ByteBuf buf) {
        Map<Integer, Integer> xp = new HashMap<>();
        Map<Integer, Integer> level = new HashMap<>();
        int xpSize = buf.readInt();
        for (int i = 0; i < xpSize; i++) xp.put(buf.readInt(), buf.readInt());
        int levelSize = buf.readInt();
        for (int i = 0; i < levelSize; i++) level.put(buf.readInt(), buf.readInt());
        return new SyncTreeXpPacket(xp, level);
    }

    public static void handle(final SyncTreeXpPacket pkt, final IPayloadContext ctx) {
        // В 1.21.1 код по умолчанию выполняется в главном потоке игры (в данном случае — клиента).
        // Метод .enqueueWork() и класс DistExecutor больше не требуются!

        // Проверяем, что пакет пришел именно на сторону клиента
        if (ctx.flow().isClientbound()) {
            // Вызываем прокси-класс для безопасного выполнения клиентского кода
            SyncTreeXpClientProxy.apply(pkt);
        }
    }
}