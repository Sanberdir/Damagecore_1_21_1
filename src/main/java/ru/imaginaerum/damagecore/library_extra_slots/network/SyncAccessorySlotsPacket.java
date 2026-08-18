package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

public record SyncAccessorySlotsPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncAccessorySlotsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("damagecore", "sync_accessory_slots"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAccessorySlotsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeNbt(packet.tag),
                    buf -> {
                        CompoundTag compound = buf.readNbt();
                        return new SyncAccessorySlotsPacket(compound != null ? compound : new CompoundTag());
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncAccessorySlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handleClient(packet.tag);
            }
        });
    }

    private static class ClientHandler {
        private static void handleClient(CompoundTag tag) {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                // ИСПРАВЛЕНО: Теперь запрашиваем хендлер напрямую из расширенного инвентаря игрока
                ItemStackHandler handler = ((IExtraSlotsInventory) player.getInventory()).damagecore$getExtraSlots();
                if (tag != null && !tag.isEmpty()) {
                    handler.deserializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), tag);
                }
            }
        }
    }
}
