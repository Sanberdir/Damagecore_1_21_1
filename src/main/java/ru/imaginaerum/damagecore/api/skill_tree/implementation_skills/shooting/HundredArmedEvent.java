package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class HundredArmedEvent {

    private static final Map<UUID, Boolean> lastKnownState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        // В событии PlayerTickEvent.Post логика выполняется строго в конце тика (аналог Phase.END)

        // Проверка стороны: выполняем код только на логическом сервере
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // В 1.21+ вместо event.player используется метод event.getEntity()
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        boolean hasSkill = SkillTreeServerHandler.isNodeLearned(player, "hundred_armed");

        boolean previous = lastKnownState.getOrDefault(playerId, false);
        if (previous != hasSkill) {
            lastKnownState.put(playerId, hasSkill);

            // Новый синтаксис отправки пакетов в NeoForge 1.21.1
            PacketDistributor.sendToPlayer(player, new HundredArmedSyncPacket(hasSkill));
        }
    }
}