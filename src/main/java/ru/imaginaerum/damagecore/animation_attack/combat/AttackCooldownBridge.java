package ru.imaginaerum.damagecore.animation_attack.combat;

import net.minecraft.client.player.AbstractClientPlayer;

public final class AttackCooldownBridge {
    /** Полный «заряд» — как ванильный maxAttackStrength (обычно 100). */
    public static final int FULL = 100;

    public static void reset(AbstractClientPlayer player) {
        player.attackStrengthTicker = 0;
    }

    public static void tick(AbstractClientPlayer player, CombatContext ctx, long now) {
        // Заполняем индикатор пропорционально проигранной части анимации
        long total = ctx.lockedUntil - ctx.stateEnteredAt;
        if (total <= 0) {
            player.attackStrengthTicker = FULL;
            return;
        }
        float p = (float)(now - ctx.stateEnteredAt) / (float) total;
        p = Math.max(0f, Math.min(1f, p));
        player.attackStrengthTicker = (int)(p * FULL);
    }

    public static boolean isReady(AbstractClientPlayer player) {
        return player.attackStrengthTicker >= FULL;
    }
}