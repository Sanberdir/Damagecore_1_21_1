package ru.imaginaerum.damagecore.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow.ChainLightningPacket;
import ru.imaginaerum.damagecore.animation_attack.PacketBreakBlock;
import ru.imaginaerum.damagecore.api.skill_tree.LearnNodePacket;
import ru.imaginaerum.damagecore.api.skill_tree.RequestFullSyncPacket;
import ru.imaginaerum.damagecore.api.skill_tree.SyncNodeLevelsPacket;
import ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting.HundredArmedSyncPacket;
import ru.imaginaerum.damagecore.api.skill_tree.node_variant.SelectNodeVariantPacket;
import ru.imaginaerum.damagecore.api.skill_tree.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.SyncEffectSourcePayload;
import ru.imaginaerum.damagecore.events_tree.SyncTreeXpPacket;
import ru.imaginaerum.damagecore.hud.elements.DrainStaminaPacket;
import ru.imaginaerum.damagecore.hud.elements.NormalAttackPacket;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;
// ЗАМЕНЕНО: Импортируем новые классы вместо старого UseAccessorySlot
import ru.imaginaerum.damagecore.library_extra_slots.SwapTwoSlotsPacket;
import ru.imaginaerum.damagecore.library_extra_slots.SwapTwoSlotsPacketHandler;
import ru.imaginaerum.damagecore.library_extra_slots.network.*;
import ru.imaginaerum.damagecore.library_stats.StatChangePacket;
import ru.imaginaerum.damagecore.library_stats.SyncStatsPacket;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionSyncPacket;

// ИСПРАВЛЕНО: Добавлен bus = EventBusSubscriber.Bus.MOD, так как RegisterPayloadHandlersEvent работает только на шине мода
@EventBusSubscriber(modid = "damagecore", bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // ─── Клиент ← Сервер ───
        registrar.playToClient(SyncAccessorySlotsPacket.TYPE, SyncAccessorySlotsPacket.STREAM_CODEC, SyncAccessorySlotsPacket::handle);
        registrar.playToClient(SyncNodeLevelsPacket.TYPE,   SyncNodeLevelsPacket.CODEC,   SyncNodeLevelsPacket::handle);
        registrar.playToClient(SyncNodeVariantsPacket.TYPE, SyncNodeVariantsPacket.CODEC, SyncNodeVariantsPacket::handle);
        registrar.playToClient(ChainLightningPacket.TYPE,   ChainLightningPacket.CODEC,   ChainLightningPacket::handleClient);
        registrar.playToClient(FoodProtectionSyncPacket.TYPE, FoodProtectionSyncPacket.STREAM_CODEC, FoodProtectionSyncPacket::handle);
        registrar.playToClient(SyncTreeXpPacket.TYPE,       SyncTreeXpPacket.STREAM_CODEC, SyncTreeXpPacket::handle);
        registrar.playToClient(DrainStaminaPacket.TYPE,     DrainStaminaPacket.STREAM_CODEC, DrainStaminaPacket::handle);
        registrar.playToClient(SyncStatsPacket.TYPE,        SyncStatsPacket.CODEC,          SyncStatsPacket::handle);
        registrar.playToClient(HundredArmedSyncPacket.TYPE, HundredArmedSyncPacket.CODEC,   HundredArmedSyncPacket::handle);
        registrar.playToClient(SyncEffectSourcePayload.TYPE, SyncEffectSourcePayload.STREAM_CODEC, SyncEffectSourcePayload::handle);
        registrar.playToClient(SyncCombatModePacket.TYPE, SyncCombatModePacket.STREAM_CODEC, SyncCombatModePacket::handle); // ДОБАВЛЕНО
// В месте, где регистрируете play-пакеты клиент->сервер:
        registrar.playToServer(PacketBreakBlock.TYPE, PacketBreakBlock.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleBreakBlock(packet, context.player())));
        // ─── Клиент → Сервер ───
        // ИСПРАВЛЕНО: Зарегистрирован новый пакет парного обмена предметов
        registrar.playToServer(SwapAccessorySlotsPacket.TYPE, SwapAccessorySlotsPacket.STREAM_CODEC, SwapAccessorySlotsPacketHandler::handle);
        registrar.playToServer(SwapTwoSlotsPacket.TYPE, SwapTwoSlotsPacket.STREAM_CODEC, SwapTwoSlotsPacketHandler::handle);
        registrar.playToServer(SwapOffhandWithSlotZeroPacket.TYPE, SwapOffhandWithSlotZeroPacket.STREAM_CODEC, SwapOffhandWithSlotZeroPacketHandler::handle);
        registrar.playToServer(ThirstDamagePacket.TYPE,     ThirstDamagePacket.STREAM_CODEC, ThirstDamagePacket::handle);
        registrar.playToServer(RequestFullSyncPacket.TYPE,  RequestFullSyncPacket.CODEC,    RequestFullSyncPacket::handle);
        registrar.playToServer(LearnNodePacket.TYPE,        LearnNodePacket.CODEC,          LearnNodePacket::handle);
        registrar.playToServer(SelectNodeVariantPacket.TYPE, SelectNodeVariantPacket.CODEC, SelectNodeVariantPacket::handle);
        registrar.playToServer(StatChangePacket.TYPE,       StatChangePacket.CODEC,         StatChangePacket::handle);
        registrar.playToServer(PacketTypedAttack.TYPE,      PacketTypedAttack.STREAM_CODEC, PacketTypedAttack::handle);

        // ─── Двунаправленные ───
        registrar.playBidirectional(NormalAttackPacket.TYPE, NormalAttackPacket.STREAM_CODEC, NormalAttackPacket::handle);
    }

    // ─── Удобные методы отправки ───

    public static void sendToClient(DrainStaminaPacket msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }
    private static void handleBreakBlock(PacketBreakBlock packet, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        var level = serverPlayer.level();
        var pos = packet.pos();

        // Базовая защита от читеров: дистанция и твёрдость проверяем на сервере,
        // а не доверяем слепо клиенту.
        if (serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8 * 8) return;

        var state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getDestroySpeed(level, pos) != 0.0F) return;

        // dropItems=true — обычные дропы блока (у травы обычно их и так нет);
        // передаём serverPlayer, чтобы САМ ломающий игрок не получил дублирующийся
        // levelEvent(2001) с сервера — у него уже есть свой локальный эффект.
        level.destroyBlock(pos, true, serverPlayer);
    }
    public static void sendToServer(ThirstDamagePacket msg) {
        PacketDistributor.sendToServer(msg);
    }
    public static void sendToClient(SyncAccessorySlotsPacket msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    public static void sendToServer(NormalAttackPacket msg) {
        PacketDistributor.sendToServer(msg);
    }
    public static void sendToServer(PacketBreakBlock msg) {
        PacketDistributor.sendToServer(msg);
    }
    public static void sendToServer(PacketTypedAttack msg) {
        PacketDistributor.sendToServer(msg);
    }

    // Добавлен удобный хелпер для отправки вашего пакета обмена на сервер (для чистоты вызовов)
    public static void sendToServer(SwapAccessorySlotsPacket msg) {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendToClientOrServer(NormalAttackPacket msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, msg);
    }
}
