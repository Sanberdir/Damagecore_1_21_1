package ru.imaginaerum.damagecore.animation_attack.combat;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.AttackShape;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.MovementDash;
import ru.imaginaerum.damagecore.animation_attack.combat.states.CombatState;
import ru.imaginaerum.damagecore.animation_attack.combat.states.IdleState;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class CombatContext {

    public static final int MAX_PENDING_HITS = 3;

    public record ScheduledHit(long at, List<DamageType> type, double multiplier,
                               AttackShape shape, double coneAngle, double reachBonus) {}

    public final AbstractClientPlayer player;
    public CombatState state = IdleState.INSTANCE;
    public long stateEnteredAt = 0L;
    public boolean idle = true;
    public MovementDash.ActiveMove activeMove;
    public ResourceLocation weaponId;
    public int comboIndex = 0;
    public long lockedUntil = 0L;
    public boolean strongMode = false;
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
        // Если очередь переполнена — выбрасываем САМЫЙ СТАРЫЙ удар
        // (новые клики важнее для отзывчивости)
        if (pendingHits.size() >= MAX_PENDING_HITS) {
            return;
        }
        pendingHits.addLast(new ScheduledHit(
                at, entry.damageTypes().isEmpty() ? List.of(DamageType.PIERCING) : entry.damageTypes(), entry.damageMultiplier(),
                entry.shape(), entry.coneAngle(), entry.reachBonus()));
    }

    /** Забирает все удары, время которых уже наступило. */
    public void drainReady(long now, java.util.function.Consumer<ScheduledHit> consumer) {
        while (!pendingHits.isEmpty() && now >= pendingHits.peekFirst().at()) {
            consumer.accept(pendingHits.pollFirst());
        }
    }

    public void clearPendingHits() {
        pendingHits.clear();
    }
    public int pendingCount() { return pendingHits.size(); }
}