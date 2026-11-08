package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FishingRodMenu extends AbstractContainerMenu {
    private final IItemHandler inventory;

    // Конструктор для сервера
    public FishingRodMenu(int containerId, Inventory playerInventory, IItemHandler inventory) {
        super(MenuTypes.FISHING_ROD_MENU.get(), containerId);
        this.inventory = inventory;
        this.addSlots(playerInventory);
    }

    // Конструктор для клиента (получает данные с сервера)
    public FishingRodMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(4)); // 4 слота
    }

    private void addSlots(Inventory playerInventory) {
        // 4 слота квадратом 2x2. Размер слота 18x18.
        int startX = 80; // Примерная центральная позиция
        int startY = 20;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = row * 2 + col;
                int x = startX + col * 18;
                int y = startY + row * 18;
                this.addSlot(new SlotItemHandler(inventory, index, x, y));
            }
        }

        // Добавляем инвентарь игрока (стандартно, чтобы можно было перетаскивать)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Стандартная логика быстрого перемещения
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // Если слот принадлежит контейнеру (индексы 0-3), перемещаем в инвентарь игрока
        if (index < 4) {
            if (!this.moveItemStackTo(stack, 4, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        // Иначе пытаемся переместить в контейнер (слоты 0-3)
        else if (!this.moveItemStackTo(stack, 0, 4, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Меню всегда валидно, пока открыто
    }
}