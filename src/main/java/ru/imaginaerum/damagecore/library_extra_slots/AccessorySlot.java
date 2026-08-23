package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AccessorySlot extends SlotItemHandler {

    private final Player owner;

    public AccessorySlot(IItemHandler itemHandler, int index, int x, int y, Player owner) {
        super(itemHandler, index, x, y);
        this.owner = owner;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        // Прячем слот (не рендерится, не кликается, не наводится) именно
        // в CreativeModeInventoryScreen — там для него нет корректной
        // позиции/логики, отсюда дублирующиеся "призрачные" иконки и
        // конфликты с creative-пакетами. В обычном InventoryScreen работает как обычно.
        if (FMLEnvironment.dist == Dist.CLIENT && ClientScreenCheck.isCreativeInventoryOpen()) {
            return false;
        }
        return super.isActive();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.owner != null && this.owner.containerMenu != null) {
            this.owner.containerMenu.broadcastChanges();
        }
    }

    // Вынесено в отдельный класс, чтобы не тянуть Minecraft/CreativeModeInventoryScreen
    // в classloading на dedicated-сервере до проверки FMLEnvironment.dist.
    private static final class ClientScreenCheck {
        static boolean isCreativeInventoryOpen() {
            return net.minecraft.client.Minecraft.getInstance().screen
                    instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
        }
    }
}