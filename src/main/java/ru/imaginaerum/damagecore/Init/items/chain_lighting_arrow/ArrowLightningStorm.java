package ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrowLightningStorm {
    private static final List<ArrowChainInstance> ACTIVE_CHAINS = new ArrayList<>();

    // Обновленный метод: принимает уровень силы лука
    public static void start(ServerLevel level, Entity caster, Entity target, int maxJumps, int powerLevel) {
        if (target instanceof LivingEntity livingTarget) {
            ACTIVE_CHAINS.add(new ArrowChainInstance(level, caster, livingTarget, maxJumps, powerLevel));
        }
    }

    public static void tick() {
        Iterator<ArrowChainInstance> iterator = ACTIVE_CHAINS.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    private static class ArrowChainInstance {
        private final ServerLevel level;
        private final List<LivingEntity> hitEntities = new ArrayList<>();
        private LivingEntity currentTarget;
        private Vec3 lastPosition;
        private int jumpsLeft;
        private int cooldown = 0;
        private boolean isFirstStrike = true;

        // Новые переменные для расчёта урона от Силы
        private final int powerLevel;
        private float currentDamage = 4.0F; // Базовый урон молнии

        ArrowChainInstance(ServerLevel level, Entity caster, LivingEntity firstTarget, int maxJumps, int powerLevel) {
            this.level = level;
            this.currentTarget = firstTarget;
            this.jumpsLeft = maxJumps;
            this.powerLevel = powerLevel;
            this.lastPosition = firstTarget.position().add(0, firstTarget.getBbHeight() / 2, 0);

            // К базовому урону 4.0F прибавляем по +1.0F за каждый уровень Силы лука
            this.currentDamage += (float) powerLevel * 2;

            if (caster instanceof LivingEntity livingCaster) {
                this.hitEntities.add(livingCaster);
            }
        }

        boolean tick() {
            if (currentTarget == null || jumpsLeft <= 0) {
                return false;
            }

            if (cooldown-- > 0) return true;
            cooldown = 4;

            Vec3 currentPos = currentTarget.position().add(0, currentTarget.getBbHeight() / 2, 0);

            if (currentTarget.isAlive()) {
                currentTarget.getPersistentData().putBoolean("damaged_chain_light_arrow", true);

                // Наносим урон, который автоматически увеличился благодаря зачарованию лука!
                currentTarget.hurt(level.damageSources().lightningBolt(), this.currentDamage);
            }

            hitEntities.add(currentTarget);
            sendSegmentToClients(currentPos);

            if (isFirstStrike) {
                level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 0.8F, 1.2F);
                isFirstStrike = false;
            } else {
                level.playSound(null, currentPos.x, currentPos.y, currentPos.z,
                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.WEATHER, 0.5F, 1.4F + level.random.nextFloat() * 0.3F);
            }

            this.lastPosition = currentPos;

            // С каждым прыжком урон молнии плавно угасает (например, на 15%)
            this.currentDamage *= 0.85F;

            double range = 7.0;
            AABB box = currentTarget.getBoundingBox().inflate(range);

            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity.isAlive() && !hitEntities.contains(entity));

            LivingEntity nextTarget = null;
            double closestDist = Double.MAX_VALUE;

            for (LivingEntity potential : nearby) {
                double dist = currentTarget.position().distanceToSqr(potential.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    nextTarget = potential;
                }
            }

            currentTarget = nextTarget;
            jumpsLeft--;
            return true;
        }

        private void sendSegmentToClients(Vec3 currentPos) {
            PacketDistributor.sendToPlayersNear(
                    this.level,
                    null,
                    this.lastPosition.x, this.lastPosition.y, this.lastPosition.z,
                    64.0,
                    new ChainLightningPacket(this.lastPosition, currentPos)
            );
        }
    }
}
