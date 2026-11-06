package ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents;

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
import net.minecraft.world.inventory.MenuType;
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
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ChestOfWildCurrentsEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("open");

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "chests/chest_of_wild_currents"));
    private long lidOpenedAtMs;
    private static final long FIRST_OPEN_MS = 900L; // длительность first_open в мс, подгони под модель
    static final int MAX_ITEMS      = 8;
    static final int SPAWN_AT       = 14;
    static final int DISSOLVE_AT    = 38;   // начинает гаснуть свет
    static final int DISSOLVE_TICKS = 14;   // длительность затухания
    private static final RawAnimation FIRST_OPEN = RawAnimation.begin().thenPlayAndHold("first_open");
    private static final RawAnimation DROP = RawAnimation.begin().thenPlayAndHold("drop");
    private static final RawAnimation SPIT = RawAnimation.begin().then("spit", Animation.LoopType.PLAY_ONCE);
    // только клиент, в NBT не сохраняется
    private boolean lootClaimed;
    private boolean guiOpen;       // игрок держит GUI
    private boolean lidWasOpen;
    // ── поля (добавить к существующим) ──
    static final int SPIT_INTERVAL = 20;   // пауза между типами предметов в тиках
    private int spitCooldown;
    private boolean lidOpened;      // крышка реально открылась (синхронизируется)
    private int lidWaitTicks;       // сколько тиков ждём доигрывания first_open
    static final int LID_OPEN_TICKS = 18;   // длительность first_open в тиках — подгони под модель
    // сохраняется: после первого открытия крышка остаётся открытой, пока GUI не откроют и не закроют
    private boolean lidHeld;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long openedAtMs, dissolveAtMs, spawnedAtMs;
    private boolean opened;
    private boolean dissolving;   // свет гаснет
    private int dissolveStartTick;
    private boolean finished;     // теперь обычный сундук
    private boolean spawned;
    private int openTicks;
    static final float BONUS_CHANCE = 0.6f;
    static final int CONTAINER_SIZE = 45;
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("close");
    public boolean wasOpened() {
        return opened;
    }
    // только клиент, в NBT не сохраняется
    private boolean lidOpen;

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
            guiOpen = now > 0;

            if (!l.isClientSide && now == 0 && lidHeld) {
                lidHeld = false;   // GUI открыли и закрыли — крышка может закрываться
            }

            l.blockEvent(p, s.getBlock(), 1, now);

            if (!l.isClientSide) {
                setChanged();
                l.sendBlockUpdated(p, s, s, 3);
            }
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu cm
                    && cm.getContainer() == ChestOfWildCurrentsEntity.this;
        }
    };
    @Override
    public void startOpen(Player p) {
        if (!isRemoved() && !p.isSpectator() && level != null)
            openers.incrementOpeners(p, level, worldPosition, getBlockState());
    }
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new ChestMenu(MenuType.GENERIC_9x5, id, inv, this, 5);
    }
    @Override
    public void stopOpen(Player p) {
        if (!isRemoved() && !p.isSpectator() && level != null) {
            openers.decrementOpeners(p, level, worldPosition, getBlockState());
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            guiOpen = type > 0;
            return true;
        }

        return super.triggerEvent(id, type);
    }
    private final NonNullList<ItemStack> loot = NonNullList.withSize(MAX_ITEMS + 2, ItemStack.EMPTY);
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);


    public ChestOfWildCurrentsEntity(BlockPos pos, BlockState state) {
        super(DCBlockEntities.CHEST_OF_WILD_CURRENTS.get(), pos, state);
    }

    public boolean isOpened()   { return opened; }
    public boolean isFinished() { return finished; }

    /** Сервер. false, если уже открыт. */
    public boolean open(Player opener) {
        if (!(level instanceof ServerLevel sl) || opened) {
            return false;
        }

        opened = true;

        // Лут генерируется только при самом первом открытии.
        if (!lootClaimed) {
            lootClaimed = true;

            rollLoot(sl, opener);
            chickenSurprise(sl);
        }

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
        var rnd = sl.getRandom();

// слоты MAX_ITEMS и MAX_ITEMS + 1 идут после основного лута
        if (rnd.nextFloat() < BONUS_CHANCE) {
            loot.set(MAX_ITEMS, new ItemStack(Items.GOLD_INGOT, 2 + rnd.nextInt(5)));      // 2–6
        }
        if (rnd.nextFloat() < BONUS_CHANCE) {
            loot.set(MAX_ITEMS + 1, new ItemStack(Items.EMERALD, 1 + rnd.nextInt(5)));     // 1–5
        }
    }

    // ── сервер: таймлайн ──
    // ── сервер: таймлайн ──
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChestOfWildCurrentsEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.finished) {
            if (level.getGameTime() % 20 == 0) be.openers.recheckOpeners(level, pos, state);
            return;
        }
        if (!be.opened) return;

        // ждём, пока first_open доиграет — до этого ни лут не вылетает, ни свет не горит
        if (!be.lidOpened) {
            if (++be.lidWaitTicks < LID_OPEN_TICKS) return;
            be.lidOpened = true;
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
            // openTicks начинаем считать только теперь
        }

        be.openTicks++;

        // Начало вылета: включаем анимацию drop
        if (!be.spawned && be.openTicks >= SPAWN_AT) {
            be.spawned = true;
            be.spitCooldown = 0;
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
        }
// Выплёвываем по одному типу за раз
        if (be.spawned && !be.lootEmpty()) {
            if (be.spitCooldown > 0) {
                be.spitCooldown--;
            } else {
                be.spitNext(sl);
                be.spitCooldown = SPIT_INTERVAL;
                be.setChanged();
            }
        }
        if (!be.dissolving && be.spawned && be.lootEmpty() && be.openTicks >= DISSOLVE_AT) {
            be.dissolving = true;
            be.dissolveStartTick = be.openTicks;
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
            sl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.4f);
        }

        if (be.dissolving && be.openTicks - be.dissolveStartTick >= DISSOLVE_TICKS) {
            be.finished = true;
            be.setChanged();
            sl.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean lootEmpty() {
        for (ItemStack s : loot) if (!s.isEmpty()) return false;
        return true;
    }

    /** Выплёвывает следующий непустой слот лута. */
    private void spitNext(ServerLevel sl) {
        for (int k = 0; k < loot.size(); k++) {
            ItemStack s = loot.get(k);
            if (s.isEmpty()) continue;

            loot.set(k, ItemStack.EMPTY);
            triggerAnim("main", "spit");
            double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
            double y = worldPosition.getY() + 0.4;
            var rnd = sl.getRandom();
            int count = s.getCount();

            if (count <= 1) {
                // одиночный предмет — просто выплюнуть
                double ang = rnd.nextDouble() * Math.PI * 2;
                double hSpeed = 0.10 + rnd.nextDouble() * 0.06;
                double vSpeed = 0.22 + rnd.nextDouble() * 0.06;

                ItemEntity ie = new ItemEntity(sl, cx, y, cz, s.copy());
                ie.setDeltaMovement(Math.cos(ang) * hSpeed, vSpeed, Math.sin(ang) * hSpeed);
                ie.setPickUpDelay(20);
                sl.addFreshEntity(ie);

                sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6f, 1.4f);
                sl.sendParticles(ParticleTypes.POOF, cx, y, cz, 3, 0.1, 0.05, 0.1, 0.02);
            } else {
                // стак — салют: каждый предмет отдельной сущностью веером
                double offset = rnd.nextDouble() * Math.PI * 2;
                for (int j = 0; j < count; j++) {
                    ItemStack one = s.copyWithCount(1);

                    double ang = offset + (Math.PI * 2.0 / count) * j + (rnd.nextDouble() - 0.5) * 0.4;
                    double hSpeed = 0.10 + rnd.nextDouble() * 0.10;
                    double vSpeed = 0.20 + rnd.nextDouble() * 0.08;

                    ItemEntity ie = new ItemEntity(sl, cx, y, cz, one);
                    ie.setDeltaMovement(Math.cos(ang) * hSpeed, vSpeed, Math.sin(ang) * hSpeed);
                    ie.setPickUpDelay(20);
                    sl.addFreshEntity(ie);
                }

                sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9f, 1.2f);
            }
            return; // только один тип за вызов
        }
    }

    public float getGlow() {
        if (!opened || finished || !lidOpened) return 0f;
        long now = System.currentTimeMillis();
        if (now < lidOpenedAtMs) return 0f;
        float t = Mth.clamp((now - lidOpenedAtMs) / 450f, 0f, 1f);
        return t * (1f - getFade());
    }

    /** 0..1: прогресс затухания света. */
    public float getFade() {
        if (!dissolving) return 0f;
        return Mth.clamp((System.currentTimeMillis() - dissolveAtMs) / (DISSOLVE_TICKS * 50f), 0f, 1f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ChestOfWildCurrentsEntity>(this, "main", 0, state -> {

            // Сундук ещё не открывали.
            if (!opened) return PlayState.STOP;

            if (!finished) {
                if (spawned) lidWasOpen = true;   // крышка открыта, после лута её надо закрыть
                return state.setAndContinue(spawned ? DROP : FIRST_OPEN);
            }

            // GUI открыт: open.
            if (guiOpen) {
                lidWasOpen = true;
                return state.setAndContinue(OPEN);
            }

            // После лута крышка остаётся открытой, пока GUI не откроют и не закроют.
            if (lidHeld) {
                return state.setAndContinue(DROP);
            }

            // GUI только что закрыли: close.
            if (lidWasOpen) {
                return state.setAndContinue(CLOSE);
            }

            // Только что поставили или загрузили чанк: статичная закрытая поза.
            return PlayState.STOP;
        }).triggerableAnim("spit", SPIT));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── контейнер ──
    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + Damagecore_1_21_1_neo.MODID + ".chest_of_wild_currents");
    }



    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) { this.items = list; }

    @Override
    public int getContainerSize() { return CONTAINER_SIZE; }

    // ── NBT и синхронизация ──
    // ── NBT и синхронизация ──
    private void readState(CompoundTag tag) {
        boolean wasOpened     = opened;
        boolean wasDissolving = dissolving;
        boolean wasSpawned    = spawned;
        boolean wasLidOpened  = lidOpened;
        guiOpen = tag.getBoolean("guiOpen");
        opened     = tag.getBoolean("opened");
        dissolving = tag.getBoolean("dissolving");
        finished   = tag.getBoolean("finished");
        spawned    = tag.getBoolean("spawned");
        lidOpened  = tag.getBoolean("lidOpened");

        if (opened && !wasOpened) openedAtMs = System.currentTimeMillis();
        if (lidOpened && !wasLidOpened) lidOpenedAtMs = System.currentTimeMillis();
        if (dissolving && !wasDissolving) dissolveAtMs = System.currentTimeMillis();
        if (spawned && !wasSpawned && wasOpened) spawnedAtMs = System.currentTimeMillis();

        lidHeld = tag.getBoolean("lidHeld");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("opened", opened);
        tag.putBoolean("dissolving", dissolving);
        tag.putBoolean("finished", finished);
        tag.putInt("dissolveStartTick", dissolveStartTick);
        tag.putBoolean("spawned", spawned);
        tag.putBoolean("lidOpened", lidOpened);
        tag.putBoolean("lootClaimed", lootClaimed);
        tag.putBoolean("lidHeld", lidHeld);
        tag.putInt("openTicks", openTicks);
        tag.putInt("lidWaitTicks", lidWaitTicks);
        CompoundTag lootTag = new CompoundTag();
        ContainerHelper.saveAllItems(lootTag, loot, regs);
        tag.put("Loot", lootTag);

        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, regs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        readState(tag);
        openTicks     = tag.getInt("openTicks");
        lidWaitTicks  = tag.getInt("lidWaitTicks");
        lootClaimed = tag.getBoolean("lootClaimed");
        dissolveStartTick = tag.getInt("dissolveStartTick");
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
        tag.putBoolean("guiOpen", guiOpen);
        tag.putBoolean("finished", finished);
        tag.putBoolean("spawned", spawned);
        tag.putBoolean("lidOpened", lidOpened);   // клиенту нужно знать момент открытия крышки
        tag.putBoolean("lidHeld", lidHeld);
        return tag;
    }
    // ── перенос состояния в предмет и обратно ──
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        if (lootClaimed || opened || spawned || finished) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("lockbox_opened", true);

            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);

        CustomData data = input.get(DataComponents.CUSTOM_DATA);

        if (data == null) {
            return;
        }

        CompoundTag tag = data.copyTag();

        if (tag.getBoolean("lockbox_opened")) {
            lootClaimed = true;
            spawned = true;

            opened = true;
            dissolving = false;
            finished = true;

            lidOpened = true;
            lidHeld = false;

            openTicks = 0;
            lidWaitTicks = 0;

            lidWasOpen = false;
            guiOpen = false;
        }
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}