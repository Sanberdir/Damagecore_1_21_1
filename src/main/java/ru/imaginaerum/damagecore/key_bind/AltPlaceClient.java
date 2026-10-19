package ru.imaginaerum.damagecore.key_bind;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class AltPlaceClient {

    private static boolean lastSent = false;

    private static boolean modifierDown() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    // Pre: состояние уходит на сервер раньше, чем обработается клик этого тика
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            lastSent = false;
            return;
        }

        boolean down = mc.screen == null && modifierDown();
        if (down != lastSent) {
            lastSent = down;
            PacketDistributor.sendToServer(new AltPlacePayload(down));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (!modifierDown()) return;
        if (!AltPlaceCommon.hasItemInHand(event.getEntity())) return;
        event.setUseBlock(TriState.FALSE);
    }
}