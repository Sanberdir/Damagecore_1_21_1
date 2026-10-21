package ru.imaginaerum.damagecore.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class TomePunishment {
    private TomePunishment() {}

    /** mistakes - сколько ошибок уже сделано (1 = первая). */
    public static void apply(ServerPlayer p, int mistakes) {
        ServerLevel level = p.serverLevel();
        int tier = Math.min(mistakes, 6);

        p.sendSystemMessage(Component.translatable("message.damagecore.tome.punish_" + tier)
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        p.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);

        switch (tier) {
            case 1 -> {
                switch (p.getRandom().nextInt(4)) {
                    case 0 -> effect(p, MobEffects.DARKNESS, 100, 0);
                    case 1 -> effect(p, MobEffects.CONFUSION, 120, 0);
                    case 2 -> effect(p, MobEffects.HUNGER, 200, 1);
                    default -> effect(p, MobEffects.MOVEMENT_SLOWDOWN, 100, 0);
                }
            }
            case 2 -> {
                switch (p.getRandom().nextInt(3)) {
                    case 0 -> {
                        effect(p, MobEffects.BLINDNESS, 100, 0);
                        rain(level, false);
                    }
                    case 1 -> {
                        effect(p, MobEffects.CONFUSION, 200, 0);
                        effect(p, MobEffects.HUNGER, 200, 1);
                    }
                    default -> {
                        effect(p, MobEffects.DIG_SLOWDOWN, 300, 1);
                        effect(p, MobEffects.MOVEMENT_SLOWDOWN, 100, 0);
                        p.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
            }
            case 3 -> {
                switch (p.getRandom().nextInt(3)) {
                    case 0 -> {
                        lightning(p, true);
                        effect(p, MobEffects.MOVEMENT_SLOWDOWN, 160, 1);
                    }
                    case 1 -> {
                        for (int i = 0; i < 3; i++) spawnBehind(p, EntityType.SILVERFISH, 1.5 + i * 0.5);
                    }
                    default -> {
                        p.setRemainingFireTicks(80);
                        effect(p, MobEffects.HUNGER, 200, 1);
                    }
                }
            }
            case 4 -> {
                switch (p.getRandom().nextInt(3)) {
                    case 0 -> {
                        spawnBehind(p, EntityType.ZOMBIE, 2.5);
                        effect(p, MobEffects.WEAKNESS, 200, 0);
                    }
                    case 1 -> {
                        spawnBehind(p, EntityType.SKELETON, 5.0);
                        effect(p, MobEffects.BLINDNESS, 60, 0);
                    }
                    default -> {
                        spawnBehind(p, EntityType.CAVE_SPIDER, 2.0);
                        spawnBehind(p, EntityType.CAVE_SPIDER, 2.5);
                        effect(p, MobEffects.POISON, 100, 0);
                    }
                }
            }
            case 5 -> {
                switch (p.getRandom().nextInt(3)) {
                    case 0 -> {
                        spawnBehind(p, EntityType.ZOMBIE, 2.5);
                        spawnBehind(p, EntityType.SKELETON, 4.0);
                        lightning(p, false);
                    }
                    case 1 -> {
                        spawnBehind(p, EntityType.WITCH, 5.0);
                        spawnBehind(p, EntityType.CREEPER, 3.5);
                        effect(p, MobEffects.DARKNESS, 200, 0);
                    }
                    default -> {
                        spawnBehind(p, EntityType.SPIDER, 2.5);
                        spawnBehind(p, EntityType.SPIDER, 3.5);
                        effect(p, MobEffects.WEAKNESS, 200, 1);
                        rain(level, true);
                    }
                }
            }
            default -> {
                switch (p.getRandom().nextInt(3)) {
                    case 0 -> {
                        int vexes = Math.min(3 + (mistakes - 6), 6);
                        for (int i = 0; i < vexes; i++) spawnBehind(p, EntityType.VEX, 2.0 + i * 0.5);
                        effect(p, MobEffects.WITHER, 80, 0);
                        rain(level, true);
                    }
                    case 1 -> {
                        spawnBehind(p, EntityType.CREEPER, 2.5);
                        spawnBehind(p, EntityType.CREEPER, 3.5);
                        spawnBehind(p, EntityType.ZOMBIE, 4.5);
                        effect(p, MobEffects.WITHER, 60, 0);
                    }
                    default -> {
                        lightning(p, false);
                        lightning(p, false);
                        spawnBehind(p, EntityType.SKELETON, 4.0);
                        spawnBehind(p, EntityType.ZOMBIE, 3.0);
                        effect(p, MobEffects.BLINDNESS, 100, 0);
                        rain(level, true);
                    }
                }
            }
        }
    }

    /** Верно найденная стихия: ошибки уменьшаются на 1-2. */
    public static void relief(ServerPlayer p) {
        int m = p.getData(ModAttachments.TOME_MISTAKES);
        if (m <= 0) return;
        int step = 1 + p.getRandom().nextInt(2);
        p.setData(ModAttachments.TOME_MISTAKES, Math.max(0, m - step));
        p.sendSystemMessage(Component.translatable("message.damagecore.tome.relief")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private static void effect(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e,
                               int ticks, int amp) {
        p.addEffect(new MobEffectInstance(e, ticks, amp));
    }

    private static void rain(ServerLevel level, boolean thunder) {
        if (thunder ? !level.isThundering() : !level.isRaining()) {
            level.setWeatherParameters(0, 6000, true, thunder);
        }
    }

    private static void lightning(ServerPlayer p, boolean visualOnly) {
        ServerLevel level = p.serverLevel();
        double ang = p.getRandom().nextDouble() * Math.PI * 2;
        double dist = 5 + p.getRandom().nextInt(4);
        BlockPos xz = BlockPos.containing(p.getX() + Math.cos(ang) * dist, 0, p.getZ() + Math.sin(ang) * dist);
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, xz);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(pos));
        bolt.setVisualOnly(visualOnly);
        level.addFreshEntity(bolt);
    }

    private static void spawnBehind(ServerPlayer p, EntityType<? extends Mob> type, double dist) {
        ServerLevel level = p.serverLevel();
        Mob mob = type.create(level);
        if (mob == null) return;

        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        back = back.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : back.normalize();
        double x = p.getX() + back.x * dist;
        double z = p.getZ() + back.z * dist;

        mob.moveTo(x, p.getY(), z, p.getYRot() + 180.0F, 0.0F);
        if (!level.noCollision(mob)) mob.moveTo(p.getX(), p.getY(), p.getZ());
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        mob.setTarget(p);
        level.addFreshEntity(mob);
    }
}