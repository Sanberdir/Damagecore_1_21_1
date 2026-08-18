package ru.imaginaerum.damagecore.library_extra_slots;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "damagecore")
public final class ExtraSlotsCloneHandler {

    private ExtraSlotsCloneHandler() {}

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().hasData(ModAttachments.EXTRA_SLOTS)) {
            ModAttachments.ExtraSlotsHandler oldHandler =
                    (ModAttachments.ExtraSlotsHandler) event.getOriginal().getData(ModAttachments.EXTRA_SLOTS);

            ModAttachments.ExtraSlotsHandler newHandler = new ModAttachments.ExtraSlotsHandler();
            for (int i = 0; i < ModAttachments.EXTRA_SLOTS_COUNT; i++) {
                newHandler.setStackInSlot(i, oldHandler.getStackInSlot(i));
            }
            event.getEntity().setData(ModAttachments.EXTRA_SLOTS, newHandler);
        }
    }
}