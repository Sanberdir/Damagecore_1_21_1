package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.Objects;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class PotionMasterPickupEvent {

    /**
     * СОБЫТИЕ 1: Динамическое обновление инвентаря (Для Креатива, Кликов, GUI, Крафта)
     * Срабатывает каждый тик на сервере, проверяет зелья в инвентаре и выставляет им нужный лимит.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Работаем только на серверной стороне
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Раз в 5 тиков (4 раза в секунду) — этого более чем достаточно и не нагружает сервер
        if (player.tickCount % 5 != 0) return;

        if (!SkillTreeServerHandler.isNodeLearned(player, "potion_master")) return;

        int maxStack = SkillTreeServerHandler.getNodeLevel(player, "potion_master") * 3;
        if (maxStack <= 0) return;

        var inventory = player.getInventory();
        boolean changed = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) continue;

            // Если у зелья нет лимита или он не равен текущему капу перка
            Integer currentCap = stack.get(DataComponents.MAX_STACK_SIZE);
            if (currentCap == null || currentCap != maxStack) {
                stack.set(DataComponents.MAX_STACK_SIZE, maxStack);
                changed = true;
            }
        }

        // Если мы обновили компоненты у предметов в инвентаре, шлем пакет синхронизации на клиент
        if (changed) {
            player.containerMenu.broadcastChanges();
        }
    }

    /**
     * СОБЫТИЕ 2: Поднятие предметов с земли (Ваш прошлый исправленный метод)
     * Нужен для правильной склейки сущностей ItemStack при наступании на них.
     */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "potion_master")) return;

        int maxStack = SkillTreeServerHandler.getNodeLevel(player, "potion_master") * 3;
        if (maxStack <= 0) return;

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack pickedUp = itemEntity.getItem();
        if (!(pickedUp.getItem() instanceof PotionItem)) return;

        var inventory = player.getInventory();
        boolean modified = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing.isEmpty() || !(existing.getItem() instanceof PotionItem)) continue;

            if (!isSamePotionIgnoringStackLimit(existing, pickedUp)) continue;

            existing.set(DataComponents.MAX_STACK_SIZE, maxStack);

            int canAdd = maxStack - existing.getCount();
            if (canAdd <= 0) continue;

            int toAdd = Math.min(canAdd, pickedUp.getCount());
            existing.grow(toAdd);
            pickedUp.shrink(toAdd);
            modified = true;

            if (pickedUp.isEmpty()) break;
        }

        if (!pickedUp.isEmpty()) {
            int emptySlot = inventory.getFreeSlot();
            if (emptySlot != -1) {
                pickedUp.set(DataComponents.MAX_STACK_SIZE, maxStack);
                inventory.setItem(emptySlot, pickedUp.copy());
                pickedUp.setCount(0);
                modified = true;
            }
        }

        if (modified) {
            player.take(itemEntity, itemEntity.getItem().getCount());
            player.containerMenu.broadcastChanges();

            if (pickedUp.isEmpty()) {
                itemEntity.discard();
                event.setCanPickup(TriState.FALSE);
            } else {
                itemEntity.setItem(pickedUp);
            }
        }
    }

    private static boolean isSamePotionIgnoringStackLimit(ItemStack stackA, ItemStack stackB) {
        if (!stackA.is(stackB.getItem())) return false;

        PotionContents contentsA = stackA.get(DataComponents.POTION_CONTENTS);
        PotionContents contentsB = stackB.get(DataComponents.POTION_CONTENTS);
        if (!Objects.equals(contentsA, contentsB)) return false;

        return Objects.equals(stackA.get(DataComponents.CUSTOM_NAME), stackB.get(DataComponents.CUSTOM_NAME));
    }
}
