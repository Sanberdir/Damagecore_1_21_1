package ru.imaginaerum.damagecore.animation_attack.combat.states;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager.AnimEntry;
import ru.imaginaerum.damagecore.animation_attack.combat.AnimationHelper;
import ru.imaginaerum.damagecore.animation_attack.combat.AttackCooldownBridge;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.HitTimingResolver;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

import java.util.List;

public final class IdleState implements CombatState {

    public static final IdleState INSTANCE = new IdleState();
    public static final long COMBO_RESET_WINDOW_MS = 1000L;

    private IdleState() {}

    @Override
    public void onExit(CombatContext ctx, long now) {
        ctx.idle = false;
    }

    @Override
    public boolean onPrimaryDown(CombatContext ctx, LivingEntity target) {
        long now = System.currentTimeMillis();

        if (now < ctx.lockedUntil) return true;
//        if (!AttackCooldownBridge.isReady(ctx.player)) return true;

        ItemStack stack = ctx.player.getMainHandItem();
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (WeaponTypeManager.INSTANCE.getType(id) == null) return false;

        // Смена оружия — сброс комбо
        if (ctx.weaponId != null && !ctx.weaponId.equals(id)) ctx.comboIndex = 0;
        ctx.weaponId = id;

        boolean strong = ctx.strongMode;
        ctx.strongMode = false; // сбрасываем сразу, чтобы флаг не «залипал»

// ---- Замах (удержание ЛКМ) ----
        if (strong) {
            List<String> keys = WeaponAnimationManager.INSTANCE.getChargeKeysOrder(id);
            if (!keys.isEmpty()) {
                ctx.setState(ChargingState.INSTANCE, now);
                ChargingState.playSegment(ctx, keys, now);
                return true;
            }
            // нет анимаций замаха — падаем в обычный взмах
        }

        // ---- Обычная комбо-анимация ----
        List<AnimEntry> regulars = WeaponAnimationManager.INSTANCE.getRegularSwings(id);
        if (regulars.isEmpty()) return false;

        if (ctx.comboIndex >= regulars.size()) ctx.comboIndex = 0;
        AnimEntry chosen = regulars.get(ctx.comboIndex);
        ctx.comboIndex = (ctx.comboIndex + 1) % regulars.size();

        AnimationHelper.trigger(ctx.player, chosen.animation());
        ctx.lockedUntil = now + AnimationHelper.durationMs(chosen.animation());
        ctx.setState(ComboSwingState.INSTANCE, now);

        long delayMs = HitTimingResolver.resolveMs(chosen.animation());
        ctx.scheduleHit(now + delayMs, chosen);

        // ВАЖНО: даже если damage_type == null, всё равно гасим ваниль,
        // чтобы не было двойного поведения/свинга. Просто без отправки пакета.
        return true;
    }

    @Override
    public void onTick(CombatContext ctx, long now) {
        if (ctx.idle) return;
        long time = now - ctx.stateEnteredAt;
        if (time > COMBO_RESET_WINDOW_MS) {
            ctx.comboIndex = 0;
            AnimationHelper.stop(ctx.player);
            ctx.clearPendingHits();
            ctx.idle = true;
        }
    }
}