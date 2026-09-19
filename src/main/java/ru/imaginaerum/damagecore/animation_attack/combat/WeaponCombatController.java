package ru.imaginaerum.damagecore.animation_attack.combat;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.AttackHitResolver;
import ru.imaginaerum.damagecore.animation_attack.combat.states.IdleState;

public final class WeaponCombatController {

    private final CombatContext ctx;

    public WeaponCombatController(AbstractClientPlayer player) {
        this.ctx = new CombatContext(player);
        this.ctx.stateEnteredAt = System.currentTimeMillis();
    }

    public CombatContext context() { return ctx; }

    public boolean onPrimaryDown(LivingEntity target) {
        if (ctx.pendingCount() == CombatContext.MAX_PENDING_HITS) return true;
        return ctx.state.onPrimaryDown(ctx, target);
    }

    public void onPrimaryUp() {
        ctx.state.onPrimaryUp(ctx);
    }

    public void reset() {
        AnimationHelper.stop(ctx.player);
        ctx.clearPendingHits();
        ctx.comboIndex = 0;
        ctx.wantsRelease = false;
        ctx.setState(IdleState.INSTANCE, System.currentTimeMillis());
    }

    public void tick(long now) {
        ctx.state.onTick(ctx, now);

        ctx.drainReady(now, hit -> AttackHitResolver.resolve(ctx.player, hit));
        AttackCooldownBridge.tick(ctx.player, ctx, now);
    }
}