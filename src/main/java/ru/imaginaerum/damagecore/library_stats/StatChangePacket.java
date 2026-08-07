package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_stats.attributes.AttributeApplier;

// 1. Превращаем класс в record и реализуем CustomPacketPayload
// ВАЖНО: компонент рекорда назван statsType (не type!), т.к. CustomPacketPayload сам требует
// метод type(), а record для компонента с именем "type" сгенерировал бы accessor type(),
// конфликтующий по возвращаемому типу с интерфейсным методом.
public record StatChangePacket(StatsType statsType, boolean increment) implements CustomPacketPayload {

    // 2. Регистрируем уникальный идентификатор пакета
    public static final CustomPacketPayload.Type<StatChangePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "stat_change"));

    // 3. Создаем потоковый кодек вместо старых encode/decode
    public static final StreamCodec<FriendlyByteBuf, StatChangePacket> CODEC = StreamCodec.of(
            StatChangePacket::encode,
            StatChangePacket::decode
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, StatChangePacket packet) {
        buf.writeUtf(packet.statsType.getId());
        buf.writeBoolean(packet.increment);
    }

    private static StatChangePacket decode(FriendlyByteBuf buf) {
        StatsType statsType = StatsType.fromId(buf.readUtf());
        boolean increment = buf.readBoolean();
        return new StatChangePacket(statsType, increment);
    }

    private static int getServerXp(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        int xpToNext;
        if (level >= 30) {
            xpToNext = 112 + (level - 30) * 9;
        } else if (level >= 15) {
            xpToNext = 37 + (level - 15) * 5;
        } else {
            xpToNext = 7 + level * 2;
        }

        int totalForLevel;
        if (level >= 32) {
            totalForLevel = (int)(4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 17) {
            totalForLevel = (int)(2.5 * level * level - 40.5 * level + 360);
        } else {
            totalForLevel = level * level + 6 * level;
        }

        return totalForLevel + (int)(progress * xpToNext);
    }

    private static void removeXp(ServerPlayer player, int cost) {
        int remaining = cost;

        while (remaining > 0) {
            if (player.experienceProgress > 0f) {
                int levelCost = player.getXpNeededForNextLevel();
                int progressXp = (int)(player.experienceProgress * levelCost);

                if (progressXp >= remaining) {
                    player.experienceProgress -= (float) remaining / levelCost;
                    remaining = 0;
                } else {
                    remaining -= progressXp;
                    player.experienceProgress = 0f;
                }
            }

            if (remaining > 0 && player.experienceLevel > 0) {
                player.experienceLevel--;
                int levelCost = player.getXpNeededForNextLevel();
                player.experienceProgress = 1f;

                int progressXp = (int)(player.experienceProgress * levelCost);
                if (progressXp >= remaining) {
                    player.experienceProgress -= (float) remaining / levelCost;
                    remaining = 0;
                } else {
                    remaining -= progressXp;
                    player.experienceProgress = 0f;
                }
            } else {
                break;
            }
        }

        player.totalExperience = getServerXp(player);
    }

    public static void handle(final StatChangePacket packet, final IPayloadContext ctx) {
        // Проверяем, что пакет пришел от игрока на сервер
        if (!ctx.flow().isServerbound()) return;

        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player == null || packet.statsType() == null) return;

        // ✅ ИСПРАВЛЕНО: Безопасно извлекаем PlayerStats через новые Data Attachments
        var statsOptional = PlayerStatsCapability.get(player);
        if (statsOptional.isEmpty()) return;
        PlayerStats stats = statsOptional.get();

        if (packet.increment()) {
            if (stats.isMaxLevel(packet.statsType())) return;

            int cost = stats.getNextCost(packet.statsType());
            int actualXp = getServerXp(player);
            if (actualXp < cost) return;

            removeXp(player, cost);
            stats.setStat(packet.statsType(), stats.getStat(packet.statsType()) + 1);
            stats.setPressCount(packet.statsType(), stats.getPressCount(packet.statsType()) + 1);

        } else {
            if (stats.getPressCount(packet.statsType()) <= 0) return;

            int refund = stats.getRefundCost(packet.statsType());
            stats.setStat(packet.statsType(), stats.getStat(packet.statsType()) - 1);
            stats.setPressCount(packet.statsType(), stats.getPressCount(packet.statsType()) - 1);
            player.giveExperiencePoints(refund);
            player.totalExperience = getServerXp(player);
        }

        if (packet.statsType() == StatsType.LIVE_FORCE) {
            AttributeApplier.applyLiveForge(player, stats.getStat(StatsType.LIVE_FORCE));

            float newMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }

        // ✅ ИСПРАВЛЕНО: Синхронизируем обновленные данные обратно игроку
        PacketDistributor.sendToPlayer(player, new SyncStatsPacket(stats, getServerXp(player)));
    }
}