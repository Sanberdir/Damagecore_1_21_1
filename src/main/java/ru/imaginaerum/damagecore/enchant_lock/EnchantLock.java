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
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class EnchantLock {
    private EnchantLock() {}

    private static final String UNLOCK_NODE = "start_enchantment";

    public static boolean isUnlocked(ServerPlayer player) {
        return SkillTreeServerHandler.isNodeLearned(player, UNLOCK_NODE);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) return;

        // решает только сервер
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // Shift + предмет в руке = игрок ставит блок рядом
        boolean placing = sp.isSecondaryUseActive()
                && (!sp.getMainHandItem().isEmpty() || !sp.getOffhandItem().isEmpty());
        if (placing) return;

        if (isUnlocked(sp)) return;

        event.setUseBlock(TriState.FALSE);

        if (event.getHand() == InteractionHand.MAIN_HAND) {
            sp.displayClientMessage(Component.translatable("message.damagecore.enchanting_locked"), true);
        }
    }
}