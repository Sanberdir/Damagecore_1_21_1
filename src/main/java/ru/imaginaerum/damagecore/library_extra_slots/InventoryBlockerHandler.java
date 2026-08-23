package ru.imaginaerum.damagecore.library_extra_slots;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public final class InventoryBlockerHandler {

    private InventoryBlockerHandler() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        // Проверяем, что открывается именно стандартный инвентарь выживания
        if (event.getScreen() instanceof InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getWindow() == null) return;

            long windowHandle = mc.getWindow().getWindow();
            // Проверяем, зажат ли CTRL в момент попытки открытия
            boolean isCtrlDown = InputConstants.isKeyDown(windowHandle, InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(windowHandle, InputConstants.KEY_RCONTROL);

            if (isCtrlDown) {
                // Отменяем открытие ванильного инвентаря
                event.setCanceled(true);

                // Отправляем пакет на сервер для обмена слотов 2 и 3
                PacketDistributor.sendToServer(new SwapTwoSlotsPacket());
            }
        }
    }
}
