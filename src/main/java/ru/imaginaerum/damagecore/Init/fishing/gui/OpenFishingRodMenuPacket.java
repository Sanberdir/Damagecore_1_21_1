package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import net.neoforged.neoforge.items.ItemStackHandler;

public record OpenFishingRodMenuPacket() implements CustomPacketPayload {
    public static final Type<OpenFishingRodMenuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "open_fishing_menu"));
    public static final StreamCodec<FriendlyByteBuf, OpenFishingRodMenuPacket> STREAM_CODEC = StreamCodec.unit(new OpenFishingRodMenuPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenFishingRodMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Создаем контейнер на 4 слота
                ItemStackHandler handler = new ItemStackHandler(4);
                // Открываем меню для игрока
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new FishingRodMenu(id, inv, handler),
                        Component.translatable("gui.damagecore.fishing_rod")
                ));
            }
        });
    }
}