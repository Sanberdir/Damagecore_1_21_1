package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;

public record SyncCombatModePacket(int entityId, boolean active) implements CustomPacketPayload {

    public static final Type<SyncCombatModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "sync_combat_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCombatModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncCombatModePacket::entityId,
                    ByteBufCodecs.BOOL, SyncCombatModePacket::active,
                    SyncCombatModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncCombatModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            if (level.getEntity(packet.entityId()) instanceof Player targetPlayer
                    && targetPlayer instanceof ICombatModeEntity combatEntity) {
                combatEntity.damagecore$setCombatMode(packet.active());
            }
        });
    }
}