package ru.imaginaerum.damagecore.library_extra_slots.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.library_extra_slots.ModAttachments;

public final class UseAccessorySlotPacketHandler {

    private UseAccessorySlotPacketHandler() {}

    public static void handle(UseAccessorySlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (!ModAttachments.EXTRA_SLOTS.isBound()) return;

            ModAttachments.ExtraSlotsHandler handler =
                    (ModAttachments.ExtraSlotsHandler) serverPlayer.getData(ModAttachments.EXTRA_SLOTS);

            // Индекс 2 = 3-й созданный слот (нумерация с 0)
            ItemStack stack = handler.getStackInSlot(2);
            if (stack.isEmpty()) return;

            // Симулируем использование предмета (еда, зелье, и т.п.),
            // как будто он в главной руке — результат кладём обратно в тот же слот.
            InteractionResultHolder<ItemStack> result =
                    stack.use(serverPlayer.level(), serverPlayer, InteractionHand.MAIN_HAND);
            handler.setStackInSlot(2, result.getObject());
        });
    }
}