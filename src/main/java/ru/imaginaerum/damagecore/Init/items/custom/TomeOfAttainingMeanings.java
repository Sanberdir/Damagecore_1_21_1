package ru.imaginaerum.damagecore.Init.items.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.imaginaerum.damagecore.client.ClientHooks;
import ru.imaginaerum.damagecore.client.ModAttachments;

import java.util.List;

public class TomeOfAttainingMeanings extends Item {
    private static final int VISION_PARTS = 8;

    private static final int EARTH = 1;
    private static final int WATER = 2;
    private static final int FIRE = 4;
    private static final int AIR = 8;
    private static final int ALL_ELEMENTS = EARTH | WATER | FIRE | AIR;

    private static final double SKY_CHECK_DISTANCE = 10.0;
    private static final float SKY_MIN_PITCH = -30.0F; // xRot: отрицательное значение = взгляд вверх

    public TomeOfAttainingMeanings(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        int step = ClientHooks.getTomeStep();
        if (step == 6) {
            tooltip.add(Component.translatable("tooltip.damagecore.tome.vibrating")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else if (step == 7) {
            tooltip.add(Component.translatable("tooltip.damagecore.tome.calm")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    /** Клик по блоку после прочтения видения запускает поиск стихий (шаг 3). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (player.getData(ModAttachments.TOME_STEP) == 2 && player.getData(ModAttachments.TOME_VISION_READ)) {
            boolean client = context.getLevel().isClientSide();
            if (!client) {
                player.setData(ModAttachments.TOME_ELEMENTS, 0);
                player.setData(ModAttachments.TOME_STEP, 3);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int step = player.getData(ModAttachments.TOME_STEP);

        // Шаги 4 и 6: книга нечитаема; шаг 8+: пока заглушка
        if (step == 4 || step == 6 || step >= 8) {
            return InteractionResultHolder.fail(stack);
        }

        // Шаг 3: поиск стихий, текст открыть нельзя
        if (step == 3) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                searchElement(level, sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Шаги 1, 2, 5, 7: читаем книгу
        if (level.isClientSide()) {
            ClientHooks.openTomeScreen();
        } else {
            if (step == 1 && !player.getData(ModAttachments.TOME_OPENED)) {
                player.setData(ModAttachments.TOME_OPENED, true);
                player.sendSystemMessage(Component.translatable("message.damagecore.tome.open")
                        .withStyle(ChatFormatting.DARK_PURPLE));

            } else if (step == 2 && !player.getData(ModAttachments.TOME_VISION_READ)) {
                player.setData(ModAttachments.TOME_VISION_READ, true);
                for (int i = 1; i <= VISION_PARTS; i++) {
                    player.sendSystemMessage(Component.translatable("message.damagecore.tome.step_1." + i)
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---------- Поиск стихий ----------

    private void searchElement(Level level, ServerPlayer player) {
        int found = 0;

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();

            if (fluid.is(FluidTags.WATER)) {
                found = WATER;
            } else if (fluid.is(FluidTags.LAVA) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                found = FIRE;
            } else if (state.is(BlockTags.DIRT) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.SOUL_SAND)
                    || state.is(Blocks.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                found = EARTH;
            }
        } else if (isLookingAtOpenSky(level, player)) {
            found = AIR;
        }

        int mask = player.getData(ModAttachments.TOME_ELEMENTS);
        if (found == 0 || (mask & found) != 0) return; // ничего не подошло или стихия уже найдена

        mask |= found;
        player.setData(ModAttachments.TOME_ELEMENTS, mask);
        player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F); // слышит только игрок

        if (mask == ALL_ELEMENTS) {
            player.setData(ModAttachments.TOME_STEP, 4);
            player.sendSystemMessage(Component.translatable("message.damagecore.tome.finish")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** Игрок смотрит вверх, и на 10 блоков вдоль взгляда нет ни одного блока. */
    private boolean isLookingAtOpenSky(Level level, Player player) {
        if (player.getXRot() > SKY_MIN_PITCH) return false;

        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(SKY_CHECK_DISTANCE));
        BlockHitResult ray = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return ray.getType() == HitResult.Type.MISS;
    }
}