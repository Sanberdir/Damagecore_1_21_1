package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;

public final class ReleasingState implements CombatState {

    public static final ReleasingState INSTANCE = new ReleasingState();
    private ReleasingState() {}

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