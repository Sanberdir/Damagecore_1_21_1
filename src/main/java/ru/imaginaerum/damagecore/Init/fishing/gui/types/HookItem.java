package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.world.item.ItemStack;

public class HookItem extends RodPartItem {
    private final float fightTime;
    private final int wear;
    public HookItem(Properties p, float fightTime, int wear) {
        super(p, RodPartType.HOOK);
        this.fightTime = fightTime;
        this.wear = wear;
    }
    @Override public void modify(RodStats s, ItemStack st) {
        s.fightTime *= fightTime;
        s.hookWear += wear;
    }
}