package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.AttackCooldownBridge;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;

public final class ComboSwingState implements CombatState {

    public static final ComboSwingState INSTANCE = new ComboSwingState();
    private ComboSwingState() {}

    @Override
    public void onEnter(CombatContext ctx, long now) {
        AttackCooldownBridge.reset(ctx.player);
    }

    @Override
    public boolean onPrimaryDown(CombatContext ctx, LivingEntity target) {
        return true; // блокируем инпут, пока идёт взмах
    }

    @Override
    public void onTick(CombatContext ctx, long now) {
        if (now >= ctx.lockedUntil) {
            ctx.setState(IdleState.INSTANCE, now);
        }
    }
}