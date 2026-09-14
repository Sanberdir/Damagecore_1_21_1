package ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
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
        private final boolean isRed;
        // Новые переменные для расчёта урона от Силы
        private final int powerLevel;
        private float currentDamage = 8.0F; // Базовый урон молнии

        ArrowChainInstance(ServerLevel level, Entity caster, LivingEntity firstTarget, int maxJumps, int powerLevel) {
            this.level = level;
            this.currentTarget = firstTarget;
            this.jumpsLeft = maxJumps;
            this.powerLevel = powerLevel;
            this.lastPosition = firstTarget.position().add(0, firstTarget.getBbHeight() / 2, 0);

            this.currentDamage += (float) powerLevel * 2;

            // 5% шанс на красную молнию
            this.isRed = level.random.nextFloat() < 0.05F;
            if (this.isRed) {
                this.currentDamage *= 4.0F;   // урон x4
                this.jumpsLeft *= 4;          // прыжков x4
            }

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
            if (currentTarget.isAlive()) {
                // Сначала проверяем и выполняем трансформацию —
                // тогда оригинал не успеет умереть и выронить дроп
                handleMobConversion();

                currentTarget.getPersistentData().putBoolean("damaged_chain_light_arrow", true);
                currentTarget.hurt(level.damageSources().lightningBolt(), this.currentDamage);
            }
            this.lastPosition = currentPos;

            // С каждым прыжком урон молнии плавно угасает (например, на 15%)
            this.currentDamage *= 0.85F;

            double range = this.isRed ? 28.0 : 7.0;
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
        private void handleMobConversion() {
            // 1. Житель -> Ведьма
            if (currentTarget instanceof Villager villager) {
                Witch witch = EntityType.WITCH.create(level);
                if (witch != null) {
                    witch.moveTo(villager.getX(), villager.getY(), villager.getZ(),
                            villager.getYRot(), villager.getXRot());
                    witch.setCustomName(villager.getCustomName());
                    witch.setPersistenceRequired();
                    level.addFreshEntity(witch);

                    villager.skipDropExperience(); // чтобы не выпал опыт
                    villager.discard();            // discard НЕ вызывает дроп лута
                    currentTarget = witch;
                    hitEntities.add(witch);
                }
            }
            // 2. Свинья -> Зомбифицированный пиглин
            else if (currentTarget instanceof Pig pig) {
                ZombifiedPiglin piglin = EntityType.ZOMBIFIED_PIGLIN.create(level);
                if (piglin != null) {
                    piglin.moveTo(pig.getX(), pig.getY(), pig.getZ(),
                            pig.getYRot(), pig.getXRot());
                    piglin.setCustomName(pig.getCustomName());
                    piglin.setPersistenceRequired();
                    level.addFreshEntity(piglin);

                    pig.skipDropExperience();
                    pig.discard();
                    currentTarget = piglin;
                    hitEntities.add(piglin);
                }
            }
            // 3. Крипер -> Заряженный крипер
            else if (currentTarget instanceof Creeper creeper) {
                CompoundTag nbt = creeper.saveWithoutId(new CompoundTag());
                nbt.putBoolean("powered", true);
                creeper.load(nbt);
            }
        }
        private void sendSegmentToClients(Vec3 currentPos) {
            PacketDistributor.sendToPlayersNear(
                    this.level,
                    null,
                    this.lastPosition.x, this.lastPosition.y, this.lastPosition.z,
                    64.0,
                    new ChainLightningPacket(this.lastPosition, currentPos, this.isRed)
            );
        }
    }
}
