package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo; // Убедитесь, что путь верный
import ru.imaginaerum.damagecore.events_tree.SkillTreeXpManager;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.SyncStatsPacket;

public record RequestFullSyncPacket() implements CustomPacketPayload {

    // 1. Регистрируем уникальный идентификатор пакета
    public static final CustomPacketPayload.Type<RequestFullSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "request_full_sync"));

    // 2. Создаем потоковый кодек. Так как пакет пустой (нет полей), используем .unit()
    public static final StreamCodec<FriendlyByteBuf, RequestFullSyncPacket> CODEC =
            StreamCodec.unit(new RequestFullSyncPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Логика обработки пакета на стороне сервера
    // 3. Логика обработки пакета на стороне сервера
    public static void handle(final RequestFullSyncPacket pkt, final IPayloadContext ctx) {
        // Убеждаемся, что пакет пришел именно на сервер
        if (!ctx.flow().isServerbound()) return;

        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player == null) return;

        // Загружаем и синхронизируем данные дерева навыков
        SkillTreeXpManager.loadFromPersistentData(player);
        SkillTreeServerHandler.sendFullSyncToPlayer(player);

        // ✅ Безопасно получаем статы и отправляем пакет обратно игроку
        PlayerStatsCapability.get(player).ifPresent(stats -> {
            PacketDistributor.sendToPlayer(
                    player,
                    new SyncStatsPacket(stats, player.totalExperience)
            );
        });
    }

}
