package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.world.item.ItemStack;

public class ReelItem extends RodPartItem {
    private final float reelSpeed;
    public ReelItem(Properties p, float reelSpeed) {
        super(p, RodPartType.REEL);
        this.reelSpeed = reelSpeed;
    }
    @Override public void modify(RodStats s, ItemStack st) { s.reelSpeed *= reelSpeed; }
}