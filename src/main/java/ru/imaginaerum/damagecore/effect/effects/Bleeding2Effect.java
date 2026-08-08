package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffectCategory;

public class Bleeding2Effect extends BaseBleedingEffect {

    public Bleeding2Effect(MobEffectCategory category, int color) {
        super(category, color, 1.5f);
    }

    @Override
    protected int getBaseInterval() {
        return 25;
    }
}