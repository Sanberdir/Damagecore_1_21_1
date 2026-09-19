package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.AttackCooldownBridge;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;

public final class ReleasingState implements CombatState {

    public static final ReleasingState INSTANCE = new ReleasingState();
    private ReleasingState() {}

    @Override
    public void onEnter(CombatContext ctx, long now) {
        ctx.comboIndex = 0;
        ctx.wantsRelease = false;
        AttackCooldownBridge.reset(ctx.player);
    }

    @Override
    public boolean onPrimaryDown(CombatContext ctx, LivingEntity target) {
        return true; // во время релиза инпут игнорим
    }

    @Override
    public void onTick(CombatContext ctx, long now) {
        if (now >= ctx.lockedUntil) {
            ctx.setState(IdleState.INSTANCE, now);
        }
    }
}