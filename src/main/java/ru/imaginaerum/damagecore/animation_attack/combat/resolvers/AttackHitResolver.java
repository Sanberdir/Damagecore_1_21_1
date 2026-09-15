package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;

public final class AttackHitResolver {

    private static final double BASE_REACH_FALLBACK = 3.0;

    private AttackHitResolver() {}

    public static void resolve(AbstractClientPlayer player, CombatContext.ScheduledHit hit) {
        double radius = computeRadius(player, hit.reachBonus());
        switch (hit.shape()) {
            case SINGLE -> singleTarget(player, hit, radius);
            case CONE   -> coneAttack(player, hit, radius);
            case CIRCLE -> circleAttack(player, hit, radius);
        }
    }

    /** Радиус = базовый рич атрибута + бонус из JSON. */
    private static double computeRadius(AbstractClientPlayer player, double reachBonus) {
        double base;
        try {
            var attr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            base = attr != null ? attr.getValue() : BASE_REACH_FALLBACK;
        } catch (Throwable t) {
            base = BASE_REACH_FALLBACK;
        }
        return base + Math.max(0.0, reachBonus);
    }

    // ---- SINGLE (piercing) ----
    private static void singleTarget(AbstractClientPlayer player,
                                     CombatContext.ScheduledHit hit, double radius) {
        HitResult hr = Minecraft.getInstance().hitResult;
        if (!(hr instanceof EntityHitResult ehr)) return;

        Entity e = ehr.getEntity();
        if (!(e instanceof LivingEntity living) || living == player) return;

        // дистанция до центра хитбокса
        if (player.distanceTo(living) > radius + 0.5) return;
        send(player, living, hit);
    }

    // ---- CONE (slashing) ----
    private static void coneAttack(AbstractClientPlayer player,
                                   CombatContext.ScheduledHit hit, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double cosHalf = Math.cos(Math.toRadians(hit.coneAngle() * 0.5));

        for (Entity e : nearby(player, radius)) {
            if (!(e instanceof LivingEntity living) || living == player) continue;

            Vec3 targetCenter = living.position()
                    .add(0, living.getBbHeight() * 0.5, 0);
            Vec3 diff = targetCenter.subtract(eye);
            double dist = diff.length();
            if (dist > radius || dist < 0.001) continue;

            double dot = diff.scale(1.0 / dist).dot(look);
            if (dot >= cosHalf) {
                send(player, living, hit);
            }
        }
    }

    // ---- CIRCLE (круговые) ----
    private static void circleAttack(AbstractClientPlayer player,
                                     CombatContext.ScheduledHit hit, double radius) {
        for (Entity e : nearby(player, radius)) {
            if (!(e instanceof LivingEntity living) || living == player) continue;
            send(player, living, hit);
        }
    }

    private static Iterable<Entity> nearby(AbstractClientPlayer player, double radius) {
        return player.level().getEntities(player,
                player.getBoundingBox().inflate(radius + 0.5),
                e -> e instanceof LivingEntity && e.isAlive());
    }

    private static void send(AbstractClientPlayer player, LivingEntity target,
                             CombatContext.ScheduledHit hit) {
        ModNetwork.sendToServer(new PacketTypedAttack(
                target.getId(), hit.type(), hit.multiplier()));
    }
}