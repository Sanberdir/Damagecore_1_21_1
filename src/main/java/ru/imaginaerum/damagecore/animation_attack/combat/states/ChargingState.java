package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager.AnimEntry;
import ru.imaginaerum.damagecore.animation_attack.combat.AnimationHelper;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.HitTimingResolver;

import java.util.List;

public final class ChargingState implements CombatState {

    public static final ChargingState INSTANCE = new ChargingState();
    private ChargingState() {}

    @Override
    public void onEnter(CombatContext ctx, long now) {
        ctx.comboIndex = 0;
        ctx.wantsRelease = false;
    }

    @Override
    public boolean onPrimaryDown(CombatContext ctx, LivingEntity target) {
        return true; // во время замаха инпут игнорим
    }

    @Override
    public void onPrimaryUp(CombatContext ctx) {
        ctx.wantsRelease = true;
    }

    @Override
    public void onTick(CombatContext ctx, long now) {
        // Оружие выпало/сменилось — сброс
        ItemStack stack = ctx.player.getMainHandItem();
        if (stack.isEmpty()) { ctx.setState(IdleState.INSTANCE, now); return; }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.equals(ctx.weaponId)) { ctx.setState(IdleState.INSTANCE, now); return; }

        if (now < ctx.lockedUntil) return;

        List<String> keys = WeaponAnimationManager.INSTANCE.getChargeKeysOrder(ctx.weaponId);
        if (keys.isEmpty()) { ctx.setState(IdleState.INSTANCE, now); return; }

        // ---- Релиз ----
        if (ctx.wantsRelease) {
            String key = keys.get(ctx.comboIndex % keys.size());
            AnimEntry release = WeaponAnimationManager.INSTANCE.getReleaseAnimation(ctx.weaponId, key);
            ctx.wantsRelease = false;

            if (release != null) {
                AnimationHelper.trigger(ctx.player, release.animation());
                ctx.lockedUntil = now + AnimationHelper.durationMs(release.animation());
                ctx.comboIndex = (ctx.comboIndex + 1) % keys.size();
                long delayMs = HitTimingResolver.resolveMs(release);   // было: HitTimingResolver.resolveMs(release.animation())
                ctx.scheduleHit(now + delayMs, release);
                ctx.setState(ReleasingState.INSTANCE, now);
            } else {
                ctx.setState(IdleState.INSTANCE, now);
            }
            return;
        }

        // ---- Продолжаем цепочку замахов ----
        if (ctx.comboIndex < keys.size() - 1) {
            ctx.comboIndex++;
            playSegment(ctx, keys, now);
        }
        // иначе — стоим на последнем кадре, ждём релиза
    }

    static void playSegment(CombatContext ctx, List<String> keys, long now) {
        String key = keys.get(ctx.comboIndex % keys.size());
        AnimEntry charge = WeaponAnimationManager.INSTANCE.getChargeAnimation(ctx.weaponId, key);
        if (charge == null) return;
        AnimationHelper.trigger(ctx.player, charge.animation());
        ctx.lockedUntil = now + AnimationHelper.durationMs(charge.animation());
    }
}