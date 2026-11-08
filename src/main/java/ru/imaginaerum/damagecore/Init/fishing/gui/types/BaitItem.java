package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.world.item.ItemStack;

public class BaitItem extends RodPartItem {
    private final float biteChance, biteRadius;
    public BaitItem(Properties p, float biteChance, float biteRadius) {
        super(p, RodPartType.BAIT);
        this.biteChance = biteChance;
        this.biteRadius = biteRadius;
    }
    @Override public void modify(RodStats s, ItemStack st) {
        s.biteChance *= biteChance;
        s.biteRadius *= biteRadius;
    }
}