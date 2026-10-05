package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.structure_processors.RescueNetwork;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class TomeClickHandler {

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int step = mc.player.getData(ModAttachments.TOME_STEP);
        if (step != TomeLocator.COMPASS_STEP) return;

        Item tome = DCItems.TOME_OF_ATTAINING_MEANINGS.get();
        boolean holds = mc.player.getMainHandItem().is(tome)
                || mc.player.getOffhandItem().is(tome);
        if (!holds) return;

        // Отменяем ванильный pick-block полностью
        event.setCanceled(true);
        event.setSwingHand(false);

        PacketDistributor.sendToServer(new RescueNetwork.ToggleCompass());
    }
}