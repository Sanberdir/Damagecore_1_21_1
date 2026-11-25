package ru.imaginaerum.damagecore.Init.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.imaginaerum.damagecore.Init.blocks.custom.SunkenBarrelBlock;

public class SunkenBarrelBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playSound(SoundEvents.BARREL_OPEN);
            setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playSound(SoundEvents.BARREL_CLOSE);
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int old, int now) {}

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu
                    && menu.getContainer() == SunkenBarrelBlockEntity.this;
        }
    };

    public SunkenBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(DCBlockEntities.SUNKEN_BARREL.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
    }

    @Override public int getContainerSize() { return 27; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator() && level != null) {
            openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
        }
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null) {
            level.setBlock(getBlockPos(), state.setValue(SunkenBarrelBlock.OPEN, open), 3);
        }
    }

    private void playSound(SoundEvent sound) {
        if (level == null) return;
        BlockPos p = getBlockPos();
        level.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                sound, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
    }
}