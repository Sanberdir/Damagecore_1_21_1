package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.ArrayList;
import java.util.List;

public record RodSlotClickPacket(int rodSlot, int overlayIndex, int button, boolean shift)
        implements CustomPacketPayload {

    public static final Type<RodSlotClickPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "rod_slot_click"));

    public static final StreamCodec<FriendlyByteBuf, RodSlotClickPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RodSlotClickPacket::rodSlot,
            ByteBufCodecs.VAR_INT, RodSlotClickPacket::overlayIndex,
            ByteBufCodecs.VAR_INT, RodSlotClickPacket::button,
            ByteBufCodecs.BOOL, RodSlotClickPacket::shift,
            RodSlotClickPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RodSlotClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof net.minecraft.world.inventory.InventoryMenu)) return;

            if (packet.rodSlot() < 0 || packet.rodSlot() >= player.getInventory().getContainerSize()) return;
            ItemStack rod = player.getInventory().getItem(packet.rodSlot());
            if (rod.isEmpty() || !rod.is(Items.FISHING_ROD)) return;

            ItemContainerContents contents = rod.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            List<ItemStack> stacks = new ArrayList<>(4);
            for (int i = 0; i < 4; i++)
                stacks.add(i < contents.getSlots() ? contents.getStackInSlot(i).copy() : ItemStack.EMPTY);

            int idx = packet.overlayIndex();
            if (idx < 0 || idx >= 4) return;

            TagKey<Item> requiredTag = getTagForSlot(idx);
            if (requiredTag == null) return;

            ItemStack inSlot = stacks.get(idx);
            ItemStack carried = player.containerMenu.getCarried().copy();

            if (packet.shift()) {
                // Shift-клик: забираем предмет из слота
                if (!inSlot.isEmpty()) {
                    player.getInventory().add(inSlot.copy());
                    stacks.set(idx, ItemStack.EMPTY);
                }
            } else {
                if (carried.isEmpty()) {
                    // Забираем 1 предмет из слота
                    if (!inSlot.isEmpty()) {
                        player.containerMenu.setCarried(inSlot.copy());
                        stacks.set(idx, ItemStack.EMPTY);
                    }
                } else {
                    // Кладем предмет, если он подходит по тегу
                    if (carried.is(requiredTag)) {
                        // ✅ ОГРАНИЧЕНИЕ: Кладем только если слот ПУСТ
                        if (inSlot.isEmpty()) {
                            // Кладем РОВНО 1 штуку из стака
                            stacks.set(idx, carried.copyWithCount(1));
                            carried.shrink(1);
                            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                        }
                    }
                }
            }

            rod.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
            player.containerMenu.broadcastChanges();
        });
    }

    private static TagKey<Item> getTagForSlot(int index) {
        return switch (index) {
            case 0 -> ModItemTags.LINES;
            case 1 -> ModItemTags.HOOKS;
            case 2 -> ModItemTags.BAITS;
            case 3 -> ModItemTags.REELS;
            default -> null;
        };
    }
}