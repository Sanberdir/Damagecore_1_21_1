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
import ru.imaginaerum.damagecore.animation_attack.PacketBreakBlock;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;

public final class AttackHitResolver {

    private static final double BASE_REACH_FALLBACK = 3.0;
    private static final double BASE_BLOCK_REACH_FALLBACK = 4.5; // NEW: ванильный дефолт для блоков

    private AttackHitResolver() {}

    public static void resolve(AbstractClientPlayer player, CombatContext.ScheduledHit hit) {
        double radius = computeRadius(player, hit.reachBonus());
        switch (hit.shape()) {
            case SINGLE -> singleTarget(player, hit, radius);
            case CONE   -> coneAttack(player, hit, radius);
            case CIRCLE -> circleAttack(player, hit, radius);
        }
        double blockRadius = computeBlockRadius(player, hit.reachBonus());
        breakSoftBlocksInHitZone(player, hit, blockRadius); // было: breakSoftBlockInPath(...)
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

    /** Дистанция до блоков — отдельный атрибут от дистанции до сущностей. */
    private static double computeBlockRadius(AbstractClientPlayer player, double reachBonus) {
        double base;
        try {
            var attr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            base = attr != null ? attr.getValue() : BASE_BLOCK_REACH_FALLBACK;
        } catch (Throwable t) {
            base = BASE_BLOCK_REACH_FALLBACK;
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
    /**
     * Ломает мягкие блоки в зоне поражения атаки — той же геометрией (SINGLE/CONE/CIRCLE),
     * что и урон по сущностям, а не по лучу взгляда, чтобы совпадало с фактической зоной удара.
     */
    private static void breakSoftBlocksInHitZone(AbstractClientPlayer player,
                                                 CombatContext.ScheduledHit hit, double radius) {
        switch (hit.shape()) {
            case SINGLE -> breakSoftBlockAlongLook(player, radius);
            case CONE   -> breakSoftBlocksInCone(player, hit, radius);
            case CIRCLE -> breakSoftBlocksInCircle(player, radius);
        }
    }

    /** SINGLE: по-прежнему через прицел — единичная атака бьёт туда, куда смотрит игрок. */
    private static void breakSoftBlockAlongLook(AbstractClientPlayer player, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(radius));

        ClipContext ctx = new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        HitResult hr = player.level().clip(ctx);

        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) return;
        tryBreak(player, bhr.getBlockPos());
    }

    /** CONE: перебираем блоки в конусе перед игроком, как перебираются сущности в coneAttack. */
    private static void breakSoftBlocksInCone(AbstractClientPlayer player,
                                              CombatContext.ScheduledHit hit, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double cosHalf = Math.cos(Math.toRadians(hit.coneAngle() * 0.5));

        for (BlockPos pos : blocksAround(player, radius)) {
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 diff = center.subtract(eye);
            double dist = diff.length();
            if (dist > radius || dist < 0.001) continue;

            double dot = diff.scale(1.0 / dist).dot(look);
            if (dot >= cosHalf) {
                tryBreak(player, pos);
            }
        }
    }

    /** CIRCLE: все мягкие блоки вокруг игрока в радиусе. */
    private static void breakSoftBlocksInCircle(AbstractClientPlayer player, double radius) {
        for (BlockPos pos : blocksAround(player, radius)) {
            tryBreak(player, pos);
        }
    }

    /** Перебор блоков в кубе вокруг игрока — аналог nearby() для сущностей. */
    private static Iterable<BlockPos> blocksAround(AbstractClientPlayer player, double radius) {
        BlockPos center = player.blockPosition();
        int r = (int) Math.ceil(radius);
        java.util.List<BlockPos> result = new java.util.ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) { // высота удара: от -1 до +2 от ног игрока
                for (int dz = -r; dz <= r; dz++) {
                    result.add(center.offset(dx, dy, dz));
                }
            }
        }
        return result;
    }

    private static void tryBreak(AbstractClientPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness != 0.0F) return;

        breakInstantly(player, pos, state);
    }

    private static void breakInstantly(AbstractClientPlayer player, BlockPos pos, BlockState state) {
        // Локальный, немедленный визуальный отклик — 0 задержки на глаз игрока.
        var levelObj = player.level();
        if (levelObj instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
           SoundType soundType = state.getSoundType();
            clientLevel.playLocalSound(pos, soundType.getBreakSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
            clientLevel.addDestroyBlockEffect(pos, state);
            clientLevel.removeBlock(pos, false); // только визуал; сервер — источник истины
        }

        // Авторитетный слом — сервер сам решит дропы, обновит соседей,
        // без сюрпризов вроде запрета меча в креативе.
        ModNetwork.sendToServer(
                new PacketBreakBlock(pos));
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