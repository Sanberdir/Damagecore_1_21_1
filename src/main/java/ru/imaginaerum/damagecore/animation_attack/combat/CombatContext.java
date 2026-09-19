package ru.imaginaerum.damagecore.animation_attack.combat;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.AttackShape;
import ru.imaginaerum.damagecore.animation_attack.combat.states.CombatState;
import ru.imaginaerum.damagecore.animation_attack.combat.states.IdleState;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CombatContext {

    public static final int MAX_PENDING_HITS = 3;

    public record ScheduledHit(long at, DamageType type, double multiplier,
                               AttackShape shape, double coneAngle, double reachBonus) {}

    public final AbstractClientPlayer player;
    public CombatState state = IdleState.INSTANCE;
    public long stateEnteredAt = 0L;
    public boolean idle = true;

    public ResourceLocation weaponId;
    public int comboIndex = 0;
    public long lockedUntil = 0L;

    /** Выставляется в Charging, когда игрок отпустил ЛКМ. */
    public boolean wantsRelease = false;

    /** Запланированный удар (наносится по достижении at). */
    private final Deque<ScheduledHit> pendingHits = new ArrayDeque<>();

    public CombatContext(AbstractClientPlayer player) {
        this.player = player;
    }

    public void setState(CombatState newState, long now) {
        if (this.state == newState) return;
        this.state.onExit(this, now);
        this.state = newState;
        this.stateEnteredAt = now;
        newState.onEnter(this, now);
    }

    public void scheduleHit(long at, WeaponAnimationManager.AnimEntry entry) {
        System.out.printf("[CombatContext]: In %d must give %s damage\n", at, entry.damageType().getDamageName());
        // Если очередь переполнена — выбрасываем САМЫЙ СТАРЫЙ удар
        // (новые клики важнее для отзывчивости)
        if (pendingHits.size() >= MAX_PENDING_HITS) {
            System.out.println("[CombatContext]: pendingHits.size() = 3. Do nothing to put\n");
            return;
        }
        pendingHits.addLast(new ScheduledHit(
                at, entry.damageType() == null ? DamageType.PIERCING : entry.damageType(), entry.damageMultiplier(),
                entry.shape(), entry.coneAngle(), entry.reachBonus()));
    }

    /** Забирает все удары, время которых уже наступило. */
    public void drainReady(long now, java.util.function.Consumer<ScheduledHit> consumer) {
        while (!pendingHits.isEmpty() && now >= pendingHits.peekFirst().at()) {
            System.out.printf("[CombatContext]: Hit %s damage in %d\n", pendingHits.peekFirst().type().getDamageName(), pendingHits.peekFirst().at());
            consumer.accept(pendingHits.pollFirst());
        }
    }

    public void clearPendingHits() {
        System.out.printf("[CombatContext]: Clearing pendingHits. Content:\n\t%s\n", pendingHits);
        pendingHits.clear();
    }
    public int pendingCount() { return pendingHits.size(); }
}