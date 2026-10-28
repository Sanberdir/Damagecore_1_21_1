package ru.imaginaerum.damagecore.entity.sunken_lockbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SunkenLockboxEntity extends Entity implements GeoEntity {
    private static final double TARGET_DEPTH = 0.35; // насколько сундук погружён в воду
    private static final float BREAK_DAMAGE = 6f;    // накопленный урон до разрушения
    private int openTicks = 0;
    private static final EntityDataAccessor<Float> DATA_SPIN =
            SynchedEntityData.defineId(SunkenLockboxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_VISUAL =
            SynchedEntityData.defineId(SunkenLockboxEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damageTaken = 0f;
    private float spin = 0f; // градусов за тик, считается и на клиенте, и на сервере
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    public SunkenLockboxEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ---------- данные и сохранение ----------



    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPIN, 0f);
        builder.define(DATA_VISUAL, false);
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
    public boolean isFishingVisual() {
        return entityData.get(DATA_VISUAL);
    }

    public void setFishingVisual(boolean v) {
        entityData.set(DATA_VISUAL, v);
        setInvulnerable(v);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isFishingVisual(); // приманка не сохраняется в мире
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPIN.equals(key) && level().isClientSide) {
            spin = entityData.get(DATA_SPIN);
        }
    }

    private void addSpin(float v) {
        spin = Mth.clamp(spin + v, -1.5f, 1.5f);
        entityData.set(DATA_SPIN, spin);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    // клиент не должен принимать угол от сервера, иначе вернётся ступенчатость
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (isFishingVisual()) {
            lerpX = x; lerpY = y; lerpZ = z;
            lerpSteps = 2;   // при updateInterval(1) цель приходит каждый тик
        } else {
            super.lerpTo(x, y, z, getYRot(), getXRot(), steps);
        }
    }

    // ---------- взаимодействие ----------
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;   // сундук нельзя сбить, сломать или толкнуть ударом
    }

    @Override
    public boolean isPushable() { return !isFishingVisual(); }

    @Override
    public boolean isPickable() { return !isFishingVisual(); }   // топор и луч прицела проходят сквозь

    @Override
    public boolean canBeCollidedWith() { return !isFishingVisual(); }

    // ---------- движение ----------

    @Override
    public void tick() {
        super.tick();
        if (isFishingVisual()) {
            if (level().isClientSide && lerpSteps > 0) {
                double k = 1.0 / lerpSteps;
                setPos(getX() + (lerpX - getX()) * k,
                        getY() + (lerpY - getY()) * k,
                        getZ() + (lerpZ - getZ()) * k);
                lerpSteps--;
            }
            return;
        }
        if (isInWater()) {
            // едва заметный постоянный момент от волн
            spin += Mth.sin((tickCount + getId() * 37f) * 0.01f) * 0.002f;
            spin *= 0.97f;   // инерция: вращение затухает за 2-3 секунды
        } else {
            spin *= 0.8f;    // на суше быстро останавливается
        }
        setYRot(getYRot() + spin);

        Vec3 motion = getDeltaMovement();
        double depth = getFluidHeight(FluidTags.WATER);

        if (depth > 0) {
            double ny = motion.y * 0.8 + (depth - TARGET_DEPTH) * 0.1;
            motion = new Vec3(motion.x * 0.95, ny, motion.z * 0.95);

            // медленный непрерывный дрейф: направление плавно «гуляет», без рывков
            float t = tickCount + getId() * 37f;
            double angle = t * 0.004 + Mth.sin(t * 0.0023f) * 2.0;
            double force = 0.0003;
            motion = motion.add(Math.cos(angle) * force, 0, Math.sin(angle) * force);
        } else {
            double friction = onGround() ? 0.6 : 0.98; // на земле быстро тормозит
            motion = new Vec3(motion.x * friction, (motion.y - 0.04) * 0.98, motion.z * friction);
        }

        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}