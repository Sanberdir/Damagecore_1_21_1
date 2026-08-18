package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory;

@Mixin(Inventory.class)
public abstract class InventoryExtensionMixin implements IExtraSlotsInventory {

    // Создаем внутреннее поле хранилища на 3 ячейки внутри каждого объекта инвентаря
    @Unique
    private final ItemStackHandler damagecore$extraSlots = new ItemStackHandler(3);

    // Реализуем метод интерфейса-утки для получения доступа из других классов
    @Unique
    @Override
    public ItemStackHandler damagecore$getExtraSlots() {
        return this.damagecore$extraSlots;
    }
}
