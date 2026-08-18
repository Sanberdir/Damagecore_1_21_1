package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.world.entity.player.Inventory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

@EventBusSubscriber(modid = "damagecore")
public final class ExtraSlotsCloneHandler {

    private ExtraSlotsCloneHandler() {}

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Проверяем, что клонирование вызвано именно смертью или возвращением из Энда
        Inventory oldInventory = event.getOriginal().getInventory();
        Inventory newInventory = event.getEntity().getInventory();

        // Приводим ванильные инвентари к нашему интерфейсу-утке
        if (oldInventory instanceof IExtraSlotsInventory oldExtra && newInventory instanceof IExtraSlotsInventory newExtra) {
            ItemStackHandler oldHandler = oldExtra.damagecore$getExtraSlots();
            ItemStackHandler newHandler = newExtra.damagecore$getExtraSlots();

            if (oldHandler != null && newHandler != null) {
                // Копируем предметы из старой сущности игрока в новую
                for (int i = 0; i < 3; i++) {
                    newHandler.setStackInSlot(i, oldHandler.getStackInSlot(i).copy());
                }
            }
        }
    }
}
