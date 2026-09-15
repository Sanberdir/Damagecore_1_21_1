package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;

public interface CombatState {
    /** true — инпут "съеден" (ваниль отменяется). */
    default boolean onPrimaryDown(CombatContext ctx, LivingEntity target) { return false; }
    default void onPrimaryUp(CombatContext ctx) {}
    default void onTick(CombatContext ctx, long now) {}
}