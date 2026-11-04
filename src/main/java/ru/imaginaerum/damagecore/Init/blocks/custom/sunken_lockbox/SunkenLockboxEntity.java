package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.entity.DCBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SunkenLockboxEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("open");

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "chests/sunken_lockbox"));

    static final int MAX_ITEMS      = 8;
    static final int SPAWN_AT       = 14;
    static final int DISSOLVE_AT    = 38;   // начинает гаснуть свет
    static final int DISSOLVE_TICKS = 14;   // длительность затухания
    static final double HOVER_H = 0.75;
    static final int CONTAINER_SIZE = 27;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long openedAtMs, dissolveAtMs, spawnedAtMs;
    private boolean opened;
    private boolean dissolving;   // свет гаснет
    private boolean finished;     // теперь обычный сундук
    private boolean spawned;
    private int openTicks;
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("close");

    // только клиент, в NBT не сохраняется
    private boolean lidOpen;
    private boolean lidWasOpen;

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level l, BlockPos p, BlockState s) {
            l.playSound(null, p, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 0.9f);
        }

        @Override
        protected void onClose(Level l, BlockPos p, BlockState s) {
            l.playSound(null, p, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8f, 0.9f);
        }

        @Override
        protected void openerCountChanged(Level l, BlockPos p, BlockState s, int old, int now) {
            l.blockEvent(p, s.getBlock(), 1, now);   // клиенту: сколько игроков держит GUI
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu cm
                    && cm.getContainer() == SunkenLockboxEntity.this;
        }
    };
    @Override
    public void startOpen(Player p) {
        if (!isRemoved() && !p.isSpectator() && level != null)
            openers.incrementOpeners(p, level, worldPosition, getBlockState());
    }

    @Override
    public void stopOpen(Player p) {
        if (!isRemoved() && !p.isSpectator() && level != null)
            openers.decrementOpeners(p, level, worldPosition, getBlockState());
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            lidOpen = type > 0;
            return true;
        }
        return super.triggerEvent(id, type);
    }
    private final NonNullList<ItemStack> loot = NonNullList.withSize(MAX_ITEMS, ItemStack.EMPTY);
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);


    public SunkenLockboxEntity(BlockPos pos, BlockState state) {
        super(DCBlockEntities.SUNKEN_LOCKBOX.get(), pos, state);
    }

    public boolean isOpened()   { return opened; }
    public boolean isFinished() { return finished; }

    /** Сервер. false, если уже открыт. */
    public boolean open(Player opener) {
        if (!(level instanceof ServerLevel sl) || opened) return false;
        opened = true;
        rollLoot(sl, opener);
        chickenSurprise(sl);
        setChanged();
        sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
    private void chickenSurprise(ServerLevel sl) {
        AABB top = new AABB(
                worldPosition.getX(), worldPosition.getY() + 0.4, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1.2, worldPosition.getZ() + 1);

        for (Chicken chicken : sl.getEntitiesOfClass(Chicken.class, top)) {
            if (sl.getRandom().nextInt(500) != 0) continue;

            double x = chicken.getX(), y = chicken.getY(), z = chicken.getZ();
            sl.playSound(null, x, y, z, SoundEvents.CHICKEN_HURT, SoundSource.NEUTRAL, 1.0f, 1.0f);
            sl.sendParticles(ParticleTypes.POOF, x, y + 0.3, z, 8, 0.15, 0.15, 0.15, 0.02);
            chicken.discard();

            ItemEntity meat = new ItemEntity(sl, x, y + 0.3, z, new ItemStack(Items.COOKED_RABBIT));
            meat.setDeltaMovement(0, 0.2, 0);
            sl.addFreshEntity(meat);
        }
    }
    private void rollLoot(ServerLevel sl, Player opener) {
        LootParams.Builder b = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withLuck(opener != null ? opener.getLuck() : 0f);
        if (opener != null) b.withParameter(LootContextParams.THIS_ENTITY, opener);

        LootTable table = sl.getServer().reloadableRegistries().getLootTable(LOOT);
        List<ItemStack> rolled = table.getRandomItems(b.create(LootContextParamSets.CHEST));
        for (int i = 0; i < Math.min(MAX_ITEMS, rolled.size()); i++) loot.set(i, rolled.get(i));
    }

    // ── сервер: таймлайн ──
    public static void serverTick(Level level, BlockPos pos, BlockState state, SunkenLockboxEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.finished) {
            // если игрок вышел из игры, не закрыв GUI, крышка всё равно закроется
            if (level.getGameTime() % 20 == 0) be.openers.recheckOpeners(level, pos, state);
            return;
        }
        if (!be.opened) return;
        be.openTicks++;

        if (!be.spawned && be.openTicks >= SPAWN_AT) {
            be.spawned = true;
            be.spawnItems(sl);
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);   // <- добавлено
        }

        if (!be.dissolving && be.openTicks >= DISSOLVE_AT) {
            be.dissolving = true;
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
            sl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.4f);
        }

        if (be.dissolving && be.openTicks - DISSOLVE_AT >= DISSOLVE_TICKS) {
            be.finished = true;           // сундук остаётся, дальше он обычный
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void spawnItems(ServerLevel sl) {
        int n = 0;
        for (ItemStack s : loot) if (!s.isEmpty()) n++;
        if (n == 0) return;

        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        double y = worldPosition.getY() + 0.4;   // примерно уровень проёма
        var rnd = sl.getRandom();
        double offset = rnd.nextDouble() * Math.PI * 2;   // общий поворот разлёта
        int i = 0;

        for (int k = 0; k < loot.size(); k++) {
            ItemStack s = loot.get(k);
            if (s.isEmpty()) continue;

            double ang = offset + (Math.PI * 2.0 / n) * i + (rnd.nextDouble() - 0.5) * 0.4;
            double hSpeed = 0.10 + rnd.nextDouble() * 0.10;   // дальность разлёта
            double vSpeed = 0.20 + rnd.nextDouble() * 0.08;   // небольшой подброс

            ItemEntity ie = new ItemEntity(sl, cx, y, cz, s.copy());
            ie.setDeltaMovement(Math.cos(ang) * hSpeed, vSpeed, Math.sin(ang) * hSpeed);
            ie.setPickUpDelay(20);
            sl.addFreshEntity(ie);

            loot.set(k, ItemStack.EMPTY);
            i++;
        }

        sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9f, 1.2f);
        sl.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()),
                cx, y, cz,
                24,            // количество
                0.15, 0.05, 0.15,  // разброс по X, Y, Z
                0.15);         // скорость разлёта
    }

    // ── клиент: визуал ──
    /** 0..1: сила лучей. После finished всегда 0. */
    public float getGlow() {
        if (!opened || finished) return 0f;
        float t = Mth.clamp((System.currentTimeMillis() - openedAtMs - 200L) / 450f, 0f, 1f);
        return t * (1f - getFade());
    }

    /** 0..1: прогресс затухания света. */
    public float getFade() {
        if (!dissolving) return 0f;
        return Mth.clamp((System.currentTimeMillis() - dissolveAtMs) / (DISSOLVE_TICKS * 50f), 0f, 1f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            if (!opened) return PlayState.STOP;
            if (!finished) return state.setAndContinue(OPEN);          // первое открытие
            if (lidOpen) { lidWasOpen = true; return state.setAndContinue(OPEN); }
            if (lidWasOpen) return state.setAndContinue(CLOSE);        // GUI закрыли
            return PlayState.STOP;                                     // установлена закрытой
        }));
    }
    static final long PULSE_DELAY_MS = 150L;  // через сколько после открытия начать
    static final long PULSE_MS = 400L;  // длительность "пульса"

    /** Клиент. Масштаб сундука при появлении света: сжатие, затем рост с лёгким перелётом до 1.0. */
    public float getOpenScale() {
        if (!spawned || finished) return 1f;
        float t = (System.currentTimeMillis() - spawnedAtMs) / (float) PULSE_MS;
        if (t <= 0f || t >= 1f) return 1f;
        return 1f - 0.15f * Mth.sin(t * Mth.TWO_PI) * (1f - t);
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── контейнер ──
    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + Damagecore_1_21_1_neo.MODID + ".sunken_lockbox");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) { this.items = list; }

    @Override
    public int getContainerSize() { return CONTAINER_SIZE; }

    // ── NBT и синхронизация ──
    private void readState(CompoundTag tag) {
        boolean wasOpened = opened, wasDissolving = dissolving, wasFinished = finished, wasSpawned = spawned;
        opened = tag.getBoolean("opened");
        dissolving = tag.getBoolean("dissolving");
        finished = tag.getBoolean("finished");
        spawned = tag.getBoolean("spawned");
        if (opened && !wasOpened) openedAtMs = System.currentTimeMillis();
        if (dissolving && !wasDissolving) dissolveAtMs = System.currentTimeMillis();
        // пульс только если видим сам момент выброса, а не загрузку чанка
        if (spawned && !wasSpawned && wasOpened) spawnedAtMs = System.currentTimeMillis();
        if (finished && !wasFinished && wasOpened) lidOpen = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("opened", opened);
        tag.putBoolean("dissolving", dissolving);
        tag.putBoolean("finished", finished);
        tag.putBoolean("spawned", spawned);
        tag.putInt("openTicks", openTicks);

        CompoundTag lootTag = new CompoundTag();
        ContainerHelper.saveAllItems(lootTag, loot, regs);
        tag.put("Loot", lootTag);

        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, regs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        readState(tag);
        spawned = tag.getBoolean("spawned");
        openTicks = tag.getInt("openTicks");

        loot.clear();
        ContainerHelper.loadAllItems(tag.getCompound("Loot"), loot, regs);

        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, regs);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.putBoolean("opened", opened);
        tag.putBoolean("dissolving", dissolving);
        tag.putBoolean("finished", finished);
        tag.putBoolean("spawned", spawned);   // <- добавлено
        return tag;
    }
    // ── перенос состояния в предмет и обратно ──
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (finished) {
            CompoundTag t = new CompoundTag();
            t.putBoolean("lockbox_finished", true);
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().getBoolean("lockbox_finished")) {
            opened = true;
            dissolving = true;
            spawned = true;
            finished = true;
            openTicks = DISSOLVE_AT + DISSOLVE_TICKS;
        }
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}