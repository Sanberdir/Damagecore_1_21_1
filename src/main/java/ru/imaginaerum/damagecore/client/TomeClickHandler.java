package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.structure_processors.RescueNetwork;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class TomeClickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int step = mc.player.getData(ModAttachments.TOME_STEP);
        if (step != 3 && step != TomeLocator.COMPASS_STEP) return;

        Item tome = DCItems.TOME_OF_ATTAINING_MEANINGS.get();
        boolean holds = mc.player.getMainHandItem().is(tome)
                || mc.player.getOffhandItem().is(tome);
        if (!holds) return;

        // consumeClick забирает нажатие, поэтому ванильный pick-block не сработает
        while (mc.options.keyPickItem.consumeClick()) {
            PacketDistributor.sendToServer(new RescueNetwork.ToggleCompass());
        }
    }
}