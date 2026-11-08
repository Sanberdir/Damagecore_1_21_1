package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class RodPartItem extends Item {
    private final RodPartType type;

    protected RodPartItem(Properties props, RodPartType type) {
        super(props);
        this.type = type;
    }

    public RodPartType type() { return type; }

    /** Применить эффект этой детали к итоговым статам. */
    public abstract void modify(RodStats stats, ItemStack stack);
}