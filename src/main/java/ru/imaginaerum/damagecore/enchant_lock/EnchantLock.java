package ru.imaginaerum.damagecore.enchant_lock;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class EnchantLock {
    private EnchantLock() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) return;

        // Shift + предмет в руке = игрок ставит блок рядом, это не «использование» стола
        var player = event.getEntity();
        boolean placing = player.isSecondaryUseActive()
                && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
        if (placing) return;

        // запрещаем только использование самого блока (GUI), предмет в руке работает как обычно
        event.setUseBlock(TriState.FALSE);

        if (!event.getLevel().isClientSide()
                && event.getHand() == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.damagecore.enchanting_locked"), true);
        }
    }
}