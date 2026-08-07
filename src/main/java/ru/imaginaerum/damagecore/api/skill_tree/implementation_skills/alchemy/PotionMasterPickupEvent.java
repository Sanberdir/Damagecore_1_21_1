package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class PotionMasterPickupEvent {

    // ВАЖНО: Используем именно внутренний класс .Pre
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        // Игрок получается через event.getPlayer()
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "potion_master")) return;

        // Сущность предмета на земле берется через event.getItemEntity()
        ItemStack pickedUp = event.getItemEntity().getItem();
        if (!(pickedUp.getItem() instanceof PotionItem)) return;

        // Сообщаем Mixin'у, кто сейчас подбирает
        PotionMasterContext.pickingUpPlayer.set(player.getUUID());
        try {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack existing = inventory.getItem(i);
                if (existing.isEmpty()) continue;

                // Проверяем совместимость предметов и их Data Components
                if (!ItemStack.isSameItemSameComponents(existing, pickedUp)) continue;

                int maxStack = SkillTreeServerHandler.getNodeLevel(player, "potion_master") * 3;

                int canAdd = maxStack - existing.getCount();
                if (canAdd <= 0) continue;

                int toAdd = Math.min(canAdd, pickedUp.getCount());
                existing.grow(toAdd);
                pickedUp.shrink(toAdd);

                if (pickedUp.isEmpty()) {
                    event.getItemEntity().discard();
                    // Внутри .Pre этот метод гарантированно существует
                    event.setCanPickup(TriState.FALSE);
                }
                return;
            }
        } finally {
            // Чистим ThreadLocal контекст
            PotionMasterContext.pickingUpPlayer.remove();
        }
    }
}
