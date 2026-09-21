package ru.imaginaerum.damagecore.animation_attack.combat;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.AttackHitResolver;
import ru.imaginaerum.damagecore.animation_attack.combat.states.IdleState;

public final class WeaponCombatController {

    /** Сколько мс нужно держать ЛКМ, чтобы началась сильная атака. */
    private static final long HOLD_THRESHOLD_MS = 250;

    private final CombatContext ctx;

    private boolean primaryHeld = false;
    private boolean holdTriggered = false;
    private long pressedAt = 0;
    private LivingEntity pressedTarget = null;

    public WeaponCombatController(AbstractClientPlayer player) {
        this.ctx = new CombatContext(player);
        this.ctx.stateEnteredAt = System.currentTimeMillis();
    }
    public boolean suppressVanillaSwing() {
        return primaryHeld || !ctx.idle;
    }
    public CombatContext context() { return ctx; }

    public boolean onPrimaryDown(LivingEntity target) {
        if (ctx.pendingCount() == CombatContext.MAX_PENDING_HITS) return true;

        // Кнопка уже зажата: повторные события (авто-повтор атаки) гасим
        if (primaryHeld) return true;

        // Из Idle начинаем отсчёт удержания, решение принимаем позже
        if (ctx.idle) {
            primaryHeld = true;
            holdTriggered = false;
            pressedAt = System.currentTimeMillis();
            pressedTarget = target;
            return true; // отменяем ванильную атаку, обычный удар запустим по отпусканию
        }

        // Не Idle (комбо, замах и т.д.) — прежнее поведение
        return ctx.state.onPrimaryDown(ctx, target);
    }

    public void onPrimaryUp() {
        if (primaryHeld) {
            boolean wasHold = holdTriggered;
            LivingEntity target = pressedTarget;
            primaryHeld = false;
            holdTriggered = false;
            pressedTarget = null;

            if (wasHold) {
                // Сильная атака: отпускание = релиз удара
                ctx.state.onPrimaryUp(ctx);
            } else {
                // Короткий клик: обычный взмах
                ctx.strongMode = false;
                ctx.state.onPrimaryDown(ctx, target);
                ctx.state.onPrimaryUp(ctx);
            }
            return;
        }
        ctx.state.onPrimaryUp(ctx);
    }

    public void reset() {
        AnimationHelper.stop(ctx.player);
        ctx.clearPendingHits();
        ctx.comboIndex = 0;
        ctx.wantsRelease = false;
        primaryHeld = false;
        holdTriggered = false;
        pressedTarget = null;
        ctx.strongMode = false;
        ctx.setState(IdleState.INSTANCE, System.currentTimeMillis());
        ctx.idle = true;
    }

    /** true, пока идёт замах/удар/релиз или ЛКМ зажата на старте. */
    public boolean isBusy() {
        return !ctx.idle;
    }

    public void tick(long now) {
        // Порог удержания достигнут: запускаем замах сильной атаки
        if (primaryHeld && !holdTriggered && ctx.idle && now - pressedAt >= HOLD_THRESHOLD_MS) {
            holdTriggered = true;
            ctx.strongMode = true;
            ctx.state.onPrimaryDown(ctx, pressedTarget);
        }

        ctx.state.onTick(ctx, now);

        ctx.drainReady(now, hit -> AttackHitResolver.resolve(ctx.player, hit));
        AttackCooldownBridge.tick(ctx.player, ctx, now);
    }
}