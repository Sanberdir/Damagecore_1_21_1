package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class StunningEffect extends MobEffect {

    public StunningEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, motion.y, 0.0);
        entity.hasImpulse = false; // важно
        entity.hurtMarked = true;
        if (entity instanceof Player player) {
            player.xxa = 0.0F;
            player.zza = 0.0F;
            player.setSprinting(false);
            player.setJumping(false);
        }
        return true; // true = эффект продолжает тикать / успешно применён
    }
}