package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.world.item.ItemStack;

public class LineItem extends RodPartItem {
    private final float strength;
    public LineItem(Properties p, float strength) {
        super(p, RodPartType.LINE);
        this.strength = strength;
    }
    @Override public void modify(RodStats s, ItemStack st) { s.lineStrength *= strength; }
}