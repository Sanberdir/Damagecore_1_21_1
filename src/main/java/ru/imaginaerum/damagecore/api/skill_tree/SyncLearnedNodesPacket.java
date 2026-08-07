package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Убраны импорты Minecraft и LocalPlayer!

public class SyncLearnedNodesPacket {
    public final int treeId;
    public final List<String> learnedIds;

    public SyncLearnedNodesPacket(int treeId, List<String> learnedIds) {
        this.treeId = treeId;
        this.learnedIds = learnedIds;
    }

    public static void encode(SyncLearnedNodesPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeInt(pkt.learnedIds.size());
        for (String s : pkt.learnedIds) buf.writeUtf(s);
    }

    public static SyncLearnedNodesPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readInt();
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf(32767));
        return new SyncLearnedNodesPacket(treeId, list);
    }

    public static void handle(final SyncLearnedNodesPacket pkt, final IPayloadContext ctx) {
        // В 1.21.1 код по умолчанию выполняется в главном потоке клиента.
        // .enqueueWork() и DistExecutor больше не требуются!

        // Проверяем, что пакет пришел на клиент
        if (ctx.flow().isClientbound()) {
            // Если ваш пакет переписан в Java Record, используйте вызовы методов: pkt.treeId() и pkt.learnedIds()
            SyncLearnedNodesClientProxy.apply(pkt.treeId, pkt.learnedIds);
        }
    }
}