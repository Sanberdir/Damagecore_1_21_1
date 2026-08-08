package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffectCategory;

public class Bleeding3Effect extends BaseBleedingEffect {

    public Bleeding3Effect(MobEffectCategory category, int color) {
        super(category, color, 2.0f);
    }

    @Override
    protected int getBaseInterval() {
        return 20;
    }
}