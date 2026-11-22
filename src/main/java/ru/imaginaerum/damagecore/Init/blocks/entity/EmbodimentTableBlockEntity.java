package ru.imaginaerum.damagecore.Init.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EmbodimentTableBlockEntity extends BlockEntity {
    public int time;
    public float flip, oFlip, flipT, flipA;
    public float open, oOpen;
    public float rot, oRot, tRot;
    private static final RandomSource RANDOM = RandomSource.create();
    public static final int XP_PER_BOTTLE = 7;
    public static final int TICKS_PER_BOTTLE = 40;
    private boolean running;
    public final ItemStackHandler inventory = createInventory(this::setChanged);
    private int target;
    private int progress;
    @Nullable
    private UUID owner;
    public EmbodimentTableBlockEntity(BlockPos pos, BlockState state) {
        super(DCBlockEntities.EMBODIMENT_TABLE.get(), pos, state);
    }
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> target;
                case 1 -> progress;
                default -> running ? 1 : 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> target = v;
                case 1 -> progress = v;
                default -> running = v != 0;
            }
        }
        @Override public int getCount() { return 3; }
    };

    public static ItemStackHandler createInventory(Runnable onChanged) {
        return new ItemStackHandler(2) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.is(Items.GLASS_BOTTLE);
            }
            @Override protected void onContentsChanged(int slot) { onChanged.run(); }
        };
    }

    /** Реальное количество очков игрока (totalExperience ванилью не поддерживается точно). */
    public static int getPlayerXp(Player p) {
        int l = p.experienceLevel;
        int base = l <= 16 ? l * l + 6 * l
                : l <= 31 ? (int) (2.5 * l * l - 40.5 * l + 360)
                : (int) (4.5 * l * l - 162.5 * l + 2220);
        return base + Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
    }
    public void requestTarget(ServerPlayer player, int wanted) {
        if (running) return;   // после старта менять нельзя
        if (target > 0 && owner != null && !owner.equals(player.getUUID())) return;

        int max = Math.min(inventory.getStackInSlot(0).getCount(),
                target + getPlayerXp(player) / XP_PER_BOTTLE);
        int n = Mth.clamp(wanted, 0, max);
        int delta = n - target;
        if (delta == 0) return;

        player.giveExperiencePoints(-delta * XP_PER_BOTTLE);

        if (target == 0) owner = player.getUUID();
        target = n;
        if (n == 0) { progress = 0; owner = null; }
        setChanged();
    }
    public void startProcess(ServerPlayer player) {
        if (running) return;
        if (owner != null && !owner.equals(player.getUUID())) return;
        if (target > 0 && target <= inventory.getStackInSlot(0).getCount() && canWork()) {
            running = true;
            setChanged();
        }
    }

    private void finish() {
        target = 0;
        progress = 0;
        running = false;
        owner = null;
    }
    private boolean canWork() {
        if (inventory.getStackInSlot(0).isEmpty()) return false;
        ItemStack out = inventory.getStackInSlot(1);
        return out.isEmpty() || (out.is(Items.EXPERIENCE_BOTTLE) && out.getCount() < out.getMaxStackSize());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EmbodimentTableBlockEntity be) {
        if (!be.running) return;

        // Выход забит: пауза до тех пор, пока игрок не заберёт готовое
        if (!be.canWork()) return;

        if (++be.progress < TICKS_PER_BOTTLE) return;
        be.progress = 0;

        be.inventory.extractItem(0, 1, false);
        ItemStack out = be.inventory.getStackInSlot(1);
        be.inventory.setStackInSlot(1, out.isEmpty()
                ? new ItemStack(Items.EXPERIENCE_BOTTLE)
                : out.copyWithCount(out.getCount() + 1));

        be.target--;
        if (be.target <= 0) be.finish();
        be.setChanged();
    }
    public void clampTarget(ServerPlayer player) {
        if (running) return;
        if (owner != null && !owner.equals(player.getUUID())) return;

        int have = inventory.getStackInSlot(0).getCount();
        if (target > have) {
            int refund = (target - have) * XP_PER_BOTTLE;
            target = have;
            if (target == 0) { progress = 0; owner = null; }
            player.giveExperiencePoints(refund);
            setChanged();
        }
    }
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
        if (target > 0 && level instanceof ServerLevel sl) {
            ExperienceOrb.award(sl, Vec3.atCenterOf(pos), target * XP_PER_BOTTLE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Target", target);
        tag.putBoolean("Running", running);
        tag.putInt("Progress", progress);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        target = tag.getInt("Target");
        progress = tag.getInt("Progress");
        running = tag.getBoolean("Running");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state, EmbodimentTableBlockEntity be) {
        be.oOpen = be.open;
        be.oRot = be.rot;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            be.tRot = (float) Mth.atan2(dz, dx);
            be.open += 0.1F;
            if (be.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float old = be.flipT;
                do {
                    be.flipT += (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (old == be.flipT);
            }
        } else {
            be.tRot += 0.02F;
            be.open -= 0.1F;
        }

        while (be.rot >= (float) Math.PI) be.rot -= (float) (Math.PI * 2);
        while (be.rot < -(float) Math.PI) be.rot += (float) (Math.PI * 2);
        while (be.tRot >= (float) Math.PI) be.tRot -= (float) (Math.PI * 2);
        while (be.tRot < -(float) Math.PI) be.tRot += (float) (Math.PI * 2);

        float diff = be.tRot - be.rot;
        while (diff >= (float) Math.PI) diff -= (float) (Math.PI * 2);
        while (diff < -(float) Math.PI) diff += (float) (Math.PI * 2);

        be.rot += diff * 0.4F;
        be.open = Mth.clamp(be.open, 0.0F, 1.0F);
        be.time++;
        be.oFlip = be.flip;
        float f = Mth.clamp((be.flipT - be.flip) * 0.4F, -0.2F, 0.2F);
        be.flipA += (f - be.flipA) * 0.9F;
        be.flip += be.flipA;
    }
}