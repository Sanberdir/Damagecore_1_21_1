package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

import net.minecraft.util.Mth;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager.AnimEntry;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.MoveDirection;

public final class MovementDash {

    private static final long DEFAULT_DURATION_MS = 150L;

    private MovementDash() {}

    public static final class ActiveMove {
        final long startAt;
        final long endAt;
        final double speedPerTick; // блоков за тик (1 тик = 50 мс)
        final double dirX, dirZ;

        ActiveMove(long startAt, long endAt, double speedPerTick, double dirX, double dirZ) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.speedPerTick = speedPerTick;
            this.dirX = dirX;
            this.dirZ = dirZ;
        }
    }

    /** Планирует рывок, если у записи анимации задана moveDistance. Направление и yaw фиксируются в момент вызова. */
    public static void scheduleFromEntry(CombatContext ctx, AnimEntry entry, long now) {
        if (entry.moveDistance() == null || entry.moveDistance() <= 0) return;

        long delay = entry.moveDelayMs() != null ? entry.moveDelayMs() : 0L;
        long duration = entry.moveDurationMs() != null ? entry.moveDurationMs() : DEFAULT_DURATION_MS;
        MoveDirection direction = entry.moveDirection() != null ? entry.moveDirection() : MoveDirection.FORWARD;

        schedule(ctx, entry.moveDistance(), direction, delay, duration, now);
    }

    public static void schedule(CombatContext ctx, double distanceBlocks, MoveDirection direction,
                                long delayMs, long durationMs, long now) {
        if (distanceBlocks <= 0 || durationMs <= 0) return;

        float yawRad = ctx.player.getYRot() * Mth.DEG_TO_RAD;
        double fwdX = -Mth.sin(yawRad), fwdZ = Mth.cos(yawRad);
        double rightX = Mth.cos(yawRad), rightZ = Mth.sin(yawRad);

        double dirX, dirZ;
        switch (direction) {
            case BACKWARD -> { dirX = -fwdX; dirZ = -fwdZ; }
            case LEFT      -> { dirX = -rightX; dirZ = -rightZ; }
            case RIGHT     -> { dirX = rightX; dirZ = rightZ; }
            default        -> { dirX = fwdX; dirZ = fwdZ; } // FORWARD
        }

        long ticks = Math.max(1, durationMs / 50);
        double speedPerTick = distanceBlocks / ticks;
        long startAt = now + Math.max(0, delayMs);
        long endAt = startAt + durationMs;

        ctx.activeMove = new ActiveMove(startAt, endAt, speedPerTick, dirX, dirZ);
    }

    /** Вызывать каждый тик из WeaponCombatController.tick(). */
    public static void tick(CombatContext ctx, long now) {
        ActiveMove m = ctx.activeMove;
        if (m == null) return;

        if (now > m.endAt) { ctx.activeMove = null; return; }
        if (now < m.startAt) return;

        var currentY = ctx.player.getDeltaMovement().y;
        ctx.player.setDeltaMovement(m.dirX * m.speedPerTick, currentY, m.dirZ * m.speedPerTick);
        ctx.player.hasImpulse = true;
    }
}