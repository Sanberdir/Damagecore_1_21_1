package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class RodGear {
    private RodGear() {}

    public static RodStats read(ItemStack rod) {
        return read(rod, false);
    }

    public static RodStats read(ItemStack rod, boolean baitSpent) {
        RodStats s = new RodStats();
        ItemContainerContents c = rod.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (RodPartType t : RodPartType.values()) {
            int i = t.ordinal();
            ItemStack st = i < c.getSlots() ? c.getStackInSlot(i) : ItemStack.EMPTY;
            if (t == RodPartType.BAIT && baitSpent) st = ItemStack.EMPTY;
            if (st.getItem() instanceof RodPartItem p && p.type() == t) p.modify(s, st);
            else t.onEmpty(s);
        }
        return s;
    }
    public static boolean consumeBait(ItemStack rod) {
        ItemContainerContents c = rod.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int i = RodPartType.BAIT.ordinal();
        if (i >= c.getSlots()) return false;

        ItemStack bait = c.getStackInSlot(i).copy();
        if (!(bait.getItem() instanceof BaitItem)) return false;
        bait.shrink(1);   // при count 0 стек становится пустым, и слот освобождается

        List<ItemStack> list = new ArrayList<>(4);
        for (int k = 0; k < 4; k++)
            list.add(k == i ? bait : (k < c.getSlots() ? c.getStackInSlot(k).copy() : ItemStack.EMPTY));
        rod.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return true;
    }
    /** Снимает прочность с крюка внутри удочки; сломанный крюк исчезает. */
    public static void wearHook(ItemStack rod, int amount) {
        if (amount <= 0) return;
        ItemContainerContents c = rod.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int i = RodPartType.HOOK.ordinal();
        if (i >= c.getSlots()) return;

        ItemStack hook = c.getStackInSlot(i).copy();
        if (!(hook.getItem() instanceof HookItem)) return;

        int dmg = hook.getDamageValue() + amount;
        if (dmg >= hook.getMaxDamage()) hook = ItemStack.EMPTY;
        else hook.setDamageValue(dmg);

        List<ItemStack> list = new ArrayList<>(4);
        for (int k = 0; k < 4; k++)
            list.add(k == i ? hook : (k < c.getSlots() ? c.getStackInSlot(k).copy() : ItemStack.EMPTY));
        rod.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }
}