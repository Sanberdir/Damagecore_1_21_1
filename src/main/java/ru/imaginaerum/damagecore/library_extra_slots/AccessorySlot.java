package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AccessorySlot extends SlotItemHandler {

    private final Player owner;

    // ИСПРАВЛЕНО: Вместо ModAttachments.ExtraSlotsHandler используем стандартный IItemHandler.
    // Это позволяет слоту работать напрямую с расширением ванильного инвентаря.
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
    public void setChanged() {
        super.setChanged();
        // Принудительно заставляем ванильный контейнер отправить изменения на клиент.
        // Так как слоты теперь сидят внутри общего инвентаря, этот вызов мгновенно
        // синхронизирует предметы без кастомных сетевых пакетов.
        if (this.owner != null && this.owner.containerMenu != null) {
            this.owner.containerMenu.broadcastChanges();
        }
    }
}
