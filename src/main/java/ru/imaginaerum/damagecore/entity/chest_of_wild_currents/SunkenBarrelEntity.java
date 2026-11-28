package ru.imaginaerum.damagecore.entity.chest_of_wild_currents;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

public class SunkenBarrelEntity extends Entity implements GeoEntity {
    public static final int LIFETIME_TICKS = 20 * 60 * 10;
    private static final float DRIFT_SPEED = 0.025f;
    private static final double FLOAT_DEPTH = 0.45;
    private static final int SINK_TICKS = 50;      // обычное погружение
    private static final int FAST_SINK_TICKS = 20; // от лодки
    private static final EntityDataAccessor<Integer> SINK_DURATION =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.INT);

    private boolean dropOnBreak; // выпадет лут, если бочку ударил игрок
    // TODO: подставьте ID таблицы, которую использует сундук
    public static final ResourceKey<LootTable> DEFAULT_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(MODID, "chests/sunken_barrel"));
    private static final int EXPLODE_ONE_IN = 5000;
    private static final float EXPLOSION_POWER = 10f;
    public float getTiltX(float partialTick) { return Mth.lerp(partialTick, prevTiltX, tiltX); }
    public float getTiltZ(float partialTick) { return Mth.lerp(partialTick, prevTiltZ, tiltZ); }
    private static final int CRITTER_ONE_IN = 1000;
    private static final EntityDataAccessor<Float> TILT_X =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TILT_Z =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.FLOAT);

    // текущие углы (плавно догоняют целевые), считаются на клиенте и на сервере
    private float tiltX, tiltZ, prevTiltX, prevTiltZ;
    @Nullable
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;
    private static final EntityDataAccessor<Boolean> SINKING =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.BOOLEAN);
    private int dryTicks;
    private boolean inspected; // игрок уже заглядывал внутрь
    private int sinkTicks; // считается и на клиенте, и на сервере
    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DRIFT_YAW =
            SynchedEntityData.defineId(SunkenBarrelEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BarrelContainer container = new BarrelContainer();
    private int age;

    public SunkenBarrelEntity(EntityType<? extends SunkenBarrelEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        if (!level.isClientSide) {
            this.entityData.set(DRIFT_YAW, this.random.nextFloat() * 360f);
            randomizeTilt(30f);
            this.lootTable = DEFAULT_LOOT;
            this.lootTableSeed = this.random.nextLong();
        }
    }
    private void randomizeTilt(float max) {
        entityData.set(TILT_X, (random.nextFloat() * 2f - 1f) * max);
        entityData.set(TILT_Z, (random.nextFloat() * 2f - 1f) * max);
    }
    /** Из бочки выскакивает курица или кролик. */
    private void releaseCritter() {
        if (!(level() instanceof ServerLevel sl)) return;

        Mob critter;
        if (random.nextBoolean()) {
            critter = EntityType.CHICKEN.create(sl);
        } else {
            critter = EntityType.RABBIT.create(sl);
        }
        if (critter == null) return;

        critter.moveTo(getX(), getY() + 0.8, getZ(), random.nextFloat() * 360f, 0f);
        critter.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.TRIGGERED, null);
        critter.setDeltaMovement(
                (random.nextDouble() - 0.5) * 0.4,
                0.5,
                (random.nextDouble() - 0.5) * 0.4);
        sl.addFreshEntity(critter);

        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        playSound(critter instanceof net.minecraft.world.entity.animal.Chicken
                ? SoundEvents.CHICKEN_AMBIENT : SoundEvents.RABBIT_JUMP, 1f, 1f);
    }
    /** Заполняет контейнер из лут-таблицы, один раз. */
    private void unpackLootTable(@Nullable Player player) {
        if (lootTable == null || !(level() instanceof ServerLevel sl)) return;

        LootTable table = sl.getServer().reloadableRegistries().getLootTable(lootTable);
        if (player instanceof ServerPlayer sp) {
            CriteriaTriggers.GENERATE_LOOT.trigger(sp, lootTable);
        }
        this.lootTable = null;

        LootParams.Builder builder = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, position());
        if (player != null) {
            builder.withLuck(player.getLuck())
                    .withParameter(LootContextParams.THIS_ENTITY, player);
        }
        table.fill(container, builder.create(LootContextParamSets.CHEST), lootTableSeed);
    }
    private boolean hasOtherViewers(Player except) {
        for (Player p : level().players()) {
            if (p != except
                    && p.containerMenu instanceof ChestMenu menu
                    && menu.getContainer() == this.container) {
                return true;
            }
        }
        return false;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OPEN, false);
        builder.define(DRIFT_YAW, 0f);
        builder.define(SINKING, false);
        builder.define(SINK_DURATION, SINK_TICKS);
        builder.define(TILT_X, 0f);
        builder.define(TILT_Z, 0f);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }
    public boolean isSinking() {
        return this.entityData.get(SINKING);
    }

    public float getSinkProgress(float partialTick) {
        return isSinking()
                ? Mth.clamp((sinkTicks + partialTick) / entityData.get(SINK_DURATION), 0f, 1f)
                : 0f;
    }
    // ------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();

        prevTiltX = tiltX;
        prevTiltZ = tiltZ;

        boolean sinking = isSinking();
        float t = tickCount;
        float phase = (getUUID().hashCode() & 0xFFFF) * 0.001f;

        float targetX, targetZ, speed;
        if (sinking) {
            targetX = entityData.get(TILT_X);
            targetZ = entityData.get(TILT_Z);
            speed = 0.08f;
        } else {
            // у каждой бочки своё медленно меняющееся положение, до ~35°
            targetX = Mth.sin(t * 0.011f + phase) * 28f + Mth.sin(t * 0.031f + phase * 2f) * 7f;
            targetZ = Mth.cos(t * 0.009f + phase * 1.7f) * 28f + Mth.cos(t * 0.027f + phase) * 7f;
            speed = 0.05f;
        }

        tiltX += (targetX - tiltX) * speed;
        tiltZ += (targetZ - tiltZ) * speed;

        if (sinking) {
            sinkTick();
            return;
        }

        if (!level().isClientSide) {
            if (++age >= LIFETIME_TICKS) {
                vanish();
                return;
            }
            if (isInWater()) dryTicks = 0;
            else if (++dryTicks > 100) {
                vanish();
                return;
            }
            if (age % 300 == 0) {
                randomizeTilt(random.nextInt(5) == 0 ? 60f : 30f); // раз в 15 сек бочка медленно переваливается
            }
            if (age % 200 == 0) {
                entityData.set(DRIFT_YAW, entityData.get(DRIFT_YAW) + (random.nextFloat() - 0.5f) * 30f);
            }
            if (isOpen() && age % 10 == 0 && !hasOtherViewers(null)) {
                entityData.set(OPEN, false);
            }
            if (hitByBoatWithPlayer()) {
                if (!inspected && random.nextInt(EXPLODE_ONE_IN) == 0) {
                    explode();
                } else {
                    startSinking(true, true); // быстрое погружение от лодки
                }
                return;
            }
        }

        Vec3 m = getDeltaMovement();
        if (isInWater()) {
            float rad = entityData.get(DRIFT_YAW) * Mth.DEG_TO_RAD;
            double tx = -Mth.sin(rad) * DRIFT_SPEED;
            double tz = Mth.cos(rad) * DRIFT_SPEED;
            double lift = (getFluidHeight(FluidTags.WATER) - FLOAT_DEPTH) * 0.06;
            m = new Vec3(m.x * 0.9 + tx * 0.1, (m.y + lift) * 0.8, m.z * 0.9 + tz * 0.1);
        } else {
            m = new Vec3(m.x * 0.98, (m.y - 0.04) * 0.98, m.z * 0.98);
        }
        setDeltaMovement(m);
        move(MoverType.SELF, m);

        if (!level().isClientSide && horizontalCollision) {
            entityData.set(DRIFT_YAW, entityData.get(DRIFT_YAW) + 90f + random.nextFloat() * 90f);
        }

        setYRot(getYRot() + 0.4f);
    }
    /** Взрыв при столкновении (тест). Содержимое пропадает вместе с бочкой. */
    private void explode() {
        // закрываем меню у всех, кто смотрит внутрь
        for (Player p : level().players()) {
            if (p.containerMenu instanceof ChestMenu menu && menu.getContainer() == this.container) {
                p.closeContainer();
            }
        }
        level().explode(this, getX(), getY() + 0.5, getZ(),
                EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
        discard();
    }
    /** Начало погружения (только сервер). */
    private void startSinking(boolean fast, boolean closeViewers) {
        if (isSinking()) return;
        entityData.set(SINK_DURATION, fast ? FAST_SINK_TICKS : SINK_TICKS);
        entityData.set(SINKING, true);
        randomizeTilt(random.nextInt(4) == 0 ? 85f : 45f);
        entityData.set(OPEN, false);
        sinkTicks = 0;

        if (closeViewers) {
            for (Player p : level().players()) {
                if (p.containerMenu instanceof ChestMenu menu && menu.getContainer() == this.container) {
                    p.closeContainer();
                }
            }
        }

        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 0.6f, fast ? 0.9f : 0.7f);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.8f, 0.9f);
        }
    }

    /** Каждый тик погружения (клиент и сервер). */
    private void sinkTick() {
        sinkTicks++;

        boolean fast = entityData.get(SINK_DURATION) == FAST_SINK_TICKS;
        Vec3 m = getDeltaMovement();
        double down = Math.max(m.y - (fast ? 0.012 : 0.004), fast ? -0.12 : -0.05);
        setDeltaMovement(m.x * 0.85, down, m.z * 0.85);
        move(MoverType.SELF, getDeltaMovement());
        setYRot(getYRot() + (fast ? 3f : 1.5f));

        // пузырьки вдоль всего погружения
        if (sinkTicks % 2 == 0) {
            level().addParticle(ParticleTypes.BUBBLE,
                    getX() + (random.nextDouble() - 0.5) * 0.6,
                    getY() + 0.6,
                    getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0, 0.1, 0);
        }
        if (sinkTicks % 10 == 0 && level().isClientSide) {
            level().addParticle(ParticleTypes.SPLASH, getX(), getY() + 0.8, getZ(), 0, 0.05, 0);
        }

        if (!level().isClientSide && sinkTicks >= entityData.get(SINK_DURATION)) {
            finishSinking();
        }
    }

    private void finishSinking() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BARREL.defaultBlockState()),
                    getX(), getY() + 0.3, getZ(), 30, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 0.3, getZ(), 15, 0.2, 0.2, 0.2, 0.1);
            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1f, 0.9f);
            sl.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1f);
        }
        discard();
    }
    private boolean hitByBoatWithPlayer() {
        for (Boat boat : level().getEntitiesOfClass(Boat.class, getBoundingBox().inflate(0.15))) {
            if (boat.getPassengers().stream().anyMatch(p -> p instanceof Player)) {
                return true;
            }
        }
        return false;
    }

    /** Тихо исчезает по таймеру. */
    private void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.5, getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        }
        discard();
    }

    /** Столкновение с лодкой: тонет и ломается как блок, содержимое пропадает. */
    private void sink() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BARREL.defaultBlockState()),
                    getX(), getY() + 0.5, getZ(), 30, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 20, 0.3, 0.2, 0.3, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1f, 0.9f);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.6f, 1f);
        }
        discard();
    }

    // ------------------------------------------------------------ взаимодействие

    /** ПКМ: открыть бочку. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (isSinking() || player.isSpectator()) return InteractionResult.PASS;
        if (!isOpen()) {
            entityData.set(OPEN, true); // модель сменится на sunken_barrel_open
            playSound(SoundEvents.BARREL_OPEN, 0.5f, level().random.nextFloat() * 0.1f + 0.9f);
        }
        unpackLootTable(player);
        if (!inspected && random.nextInt(CRITTER_ONE_IN) == 0) {
            releaseCritter();
        }
        inspected = true;
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> ChestMenu.threeRows(id, inv, container),
                Component.translatable("container.barrel")));
        return InteractionResult.CONSUME;
    }

    /** Удар (как по лодке): ломается, падает содержимое и палка-заглушка. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (isSinking()) return false;
        if (level().isClientSide || isRemoved()) return true;

        boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        if (creative) {
            playSound(SoundEvents.WOOD_BREAK, 1f, 1f);
            discard();
            return true;
        }

        startSinking(false, true);
        return true;
    }

    @Override public boolean isPickable() { return !isSinking(); }
    @Override public boolean canBeCollidedWith() { return !isSinking(); }

    // ------------------------------------------------------------ NBT

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OPEN, false);
        if (tag.contains("DriftYaw")) entityData.set(DRIFT_YAW, tag.getFloat("DriftYaw"));
        age = tag.getInt("Age");
        if (tag.contains("TiltX")) entityData.set(TILT_X, tag.getFloat("TiltX"));
        if (tag.contains("TiltZ")) entityData.set(TILT_Z, tag.getFloat("TiltZ"));
        inspected = tag.getBoolean("Inspected");
        if (tag.contains("LootTable", Tag.TAG_STRING)) {
            lootTable = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.parse(tag.getString("LootTable")));
            lootTableSeed = tag.getLong("LootTableSeed");
        } else if (tag.contains("Items", Tag.TAG_LIST)) {
            lootTable = null;
            container.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registryAccess());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("DriftYaw", entityData.get(DRIFT_YAW));
        tag.putInt("Age", age);
        tag.putFloat("TiltX", entityData.get(TILT_X));
        tag.putFloat("TiltZ", entityData.get(TILT_Z));
        tag.putBoolean("Inspected", inspected);
        if (lootTable != null) {
            tag.putString("LootTable", lootTable.location().toString());
            if (lootTableSeed != 0L) tag.putLong("LootTableSeed", lootTableSeed);
        } else {
            tag.put("Items", container.createTag(registryAccess()));
        }
    }

    // ------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------ инвентарь (27 слотов, как у бочки)

    private class BarrelContainer extends SimpleContainer {
        BarrelContainer() { super(27); }

        @Override
        public boolean stillValid(Player player) {
            return !SunkenBarrelEntity.this.isRemoved()
                    && player.distanceToSqr(SunkenBarrelEntity.this) <= 64.0;
        }

        @Override
        public void stopOpen(Player player) {
            super.stopOpen(player);
            SunkenBarrelEntity barrel = SunkenBarrelEntity.this;
            if (barrel.level().isClientSide || barrel.isRemoved()) return;

            // закрываем, только если больше никто не смотрит в инвентарь
            if (!barrel.hasOtherViewers(player)) {
                barrel.entityData.set(OPEN, false);
                barrel.playSound(SoundEvents.BARREL_CLOSE, 0.5f, 1f);
                barrel.startSinking(false, false);
            }
        }

    }
}