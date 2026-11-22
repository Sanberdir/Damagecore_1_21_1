package ru.imaginaerum.damagecore.Init.blocks.screen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;
import ru.imaginaerum.damagecore.Init.blocks.entity.EmbodimentTableBlockEntity;

public class EmbodimentTableMenu extends AbstractContainerMenu {
    private final ItemStackHandler handler;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Player player;
    public static final int BUTTON_START = 100; // ползунок использует id 0..64

    public boolean isRunning() { return data.get(2) != 0; }
    @Nullable
    private final EmbodimentTableBlockEntity be;

    /** Клиентский конструктор. */
    public EmbodimentTableMenu(int id, Inventory inv) {
        this(id, inv, null, EmbodimentTableBlockEntity.createInventory(() -> {}),
                new SimpleContainerData(3), ContainerLevelAccess.NULL);   // было 2
    }
    /** Серверный конструктор. */
    public EmbodimentTableMenu(int id, Inventory inv, EmbodimentTableBlockEntity be) {
        this(id, inv, be, be.inventory, be.data,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    private EmbodimentTableMenu(int id, Inventory inv, @Nullable EmbodimentTableBlockEntity be,
                                ItemStackHandler handler, ContainerData data, ContainerLevelAccess access) {
        super(DCMenus.EMBODIMENT_TABLE.get(), id);
        this.be = be;
        this.handler = handler;
        this.data = data;
        this.access = access;
        this.player = inv.player;
        addSlot(new SlotItemHandler(handler, 0, 80, 19) {
            @Override public boolean mayPickup(Player p) {
                return !isRunning() && super.mayPickup(p);
            }
        });
        addSlot(new SlotItemHandler(handler, 1, 80, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 114 + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(inv, c, 8 + c * 18, 172));

        addDataSlots(data);
    }
    @Override
    public void broadcastChanges() {
        if (be != null && player instanceof ServerPlayer sp) {
            be.clampTarget(sp);
        }
        super.broadcastChanges();
    }
    public int getTarget() { return data.get(0); }
    public int getProgress() { return data.get(1); }
    public int getInputCount() { return handler.getStackInSlot(0).getCount(); }

    /** Ползунок: id кнопки = желаемое число бутылочек. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;

        if (id == BUTTON_START) {
            be.startProcess(sp);
            return true;
        }
        if (id >= 0 && id <= 64) {
            be.requestTarget(sp, id);
            return true;
        }

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else if (stack.is(Items.GLASS_BOTTLE)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (index < 29) {
                if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 2, 29, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BlocksDC.EMBODIMENT_TABLE.get());
    }
}