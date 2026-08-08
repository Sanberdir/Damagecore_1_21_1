package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffectCategory;

public class BleedingEffect extends BaseBleedingEffect {

    public BleedingEffect(MobEffectCategory category, int color) {
        super(category, color, 1.0f);
    }

    @Override
    protected int getBaseInterval() {
        return 35;
    }
}