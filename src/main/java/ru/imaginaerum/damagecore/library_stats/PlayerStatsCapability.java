package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "damagecore", bus = EventBusSubscriber.Bus.GAME)
public class PlayerStatsCapability {

    // Создаем реестр для Data Attachments
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "damagecore");

    // Регистрируем вложение для игрока. copyOnDeath() автоматически сохраняет данные при смерти/возрождении
    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS_ATTACHMENT = ATTACHMENT_TYPES.register(
            "player_stats",
            () -> AttachmentType.serializable(PlayerStats::new).copyOnDeath().build()
    );

    // Метод для инициализации регистрации. Обязательно вызовите его в конструкторе вашего главного класса мода!
    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // Метод получения данных. Возвращает java.util.Optional, что совместимо со старыми вызовами .map() и .ifPresent()
    public static Optional<PlayerStats> get(Player player) {
        return Optional.of(player.getData(PLAYER_STATS_ATTACHMENT.get()));
    }

    // Привязка через AttachCapabilitiesEvent больше НЕ НУЖНА — NeoForge делает это автоматически при вызове PlayerStats::new выше

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player player   = event.getEntity();

        // Методы reviveCaps() и invalidateCaps() удалены в 1.21.1
        // Хотя флаг copyOnDeath() уже переносит данные автоматически, для кастомных типов или переходов (например из Энда)
        // мы явно переносим данные из старого игрока в нового:
        if (original.hasData(PLAYER_STATS_ATTACHMENT.get()) && player.hasData(PLAYER_STATS_ATTACHMENT.get())) {
            PlayerStats oldStats = original.getData(PLAYER_STATS_ATTACHMENT.get());
            PlayerStats newStats = player.getData(PLAYER_STATS_ATTACHMENT.get());

            for (StatsType type : StatsType.values()) {
                newStats.setStat(type, oldStats.getStat(type));
                newStats.setPressCount(type, oldStats.getPressCount(type));
            }
        }
    }
}
