package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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
        breakSoftBlockInPath(player, hit, radius); // NEW
    }

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

    /**
     * Ломает блок, в который физически бьёт оружие — точка удара находится
     * трассировкой луча взгляда до дистанции атаки, что моделирует "куда наносится удар",
     * а не то, куда наведён курсор в момент клика.
     */
    private static void breakSoftBlockInPath(AbstractClientPlayer player,
                                             CombatContext.ScheduledHit hit, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(radius));

        // OUTLINE, а не COLLIDER: у травы/саженцев/цветов нет коллизии,
        // но они выделяются в прицел именно через OUTLINE — так же, как ваниль.
        ClipContext ctx = new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        HitResult hr = player.level().clip(ctx);

        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness != 0.0F) return;

        breakInstantly(player, pos, state, bhr.getDirection());
    }

    private static void breakInstantly(AbstractClientPlayer player, BlockPos pos,
                                       BlockState state, net.minecraft.core.Direction face) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return;

        // Серверу — та же пара пакетов, что при обычной мгновенной добыче
        connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face));
        connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, face));

        var levelObj = player.level();
        if (!(levelObj instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) return;

        // Частицы и звук — напрямую, минуя levelEvent/LevelRenderer forwarding
        SoundType soundType = state.getSoundType();
        clientLevel.playLocalSound(pos, soundType.getBreakSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
        clientLevel.addDestroyBlockEffect(pos, state);

        clientLevel.removeBlock(pos, false);
        state.getBlock().destroy(clientLevel, pos, state);
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