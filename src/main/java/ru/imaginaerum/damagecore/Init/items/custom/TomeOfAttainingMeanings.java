package ru.imaginaerum.damagecore.Init.items.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import ru.imaginaerum.damagecore.client.ClientHooks;
import ru.imaginaerum.damagecore.client.ModAttachments;
import ru.imaginaerum.damagecore.client.TomeDeeds;
import ru.imaginaerum.damagecore.client.TomeLocator;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

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
        if (step == 5) {
            tooltip.add(Component.translatable("tooltip.damagecore.tome.vibrating")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else if (step == 7) {
            tooltip.add(Component.translatable("tooltip.damagecore.tome.calm")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else if (step == 3 || step == TomeLocator.COMPASS_STEP || step == TomeLocator.SPHERE_STEP) {
            tooltip.add(Component.translatable("tooltip.damagecore.tome.switch_mode")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
    @Override
    public Component getName(ItemStack stack) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            if (ClientHooks.isLensMode()) {
                return Component.translatable("item.damagecore.tome_of_attaining_meanings.lens");
            }
            if (ClientHooks.isCompassMode()) {
                return Component.translatable("item.damagecore.tome_of_attaining_meanings.compass");
            }
            if (ClientHooks.isSphereMode()) {
                return Component.translatable("item.damagecore.tome_of_attaining_meanings.sphere");
            }
        }
        return super.getName(stack);
    }
    /** Клик по блоку после прочтения видения запускает поиск стихий (шаг 3). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int step = player.getData(ModAttachments.TOME_STEP);

        if (step > TomeLocator.FINAL_STEP) {
            return InteractionResultHolder.fail(stack);
        }
        boolean reading = player.isShiftKeyDown(); // Shift = режим книги

        if (!reading) {
            // Лупа: ПКМ сканирует стихии
            if (step == 3 && player.getData(ModAttachments.TOME_LENS_ON)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    searchElement(level, sp);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            // Компас: ПКМ ничего не делает
            if (step == TomeLocator.COMPASS_STEP && player.getData(ModAttachments.TOME_COMPASS_ON)) {
                return InteractionResultHolder.fail(stack);
            }
            if (step == TomeLocator.SPHERE_STEP) {
                int g = player.getData(ModAttachments.TOME_GOOD);
                int b = player.getData(ModAttachments.TOME_BAD);
                if (g + b >= TomeDeeds.MAX) {
                    if (!level.isClientSide()) {
                        player.setData(ModAttachments.TOME_STEP, TomeLocator.FINAL_STEP);
                        level.playSound(null, player.blockPosition(), DCSoundEvents.LEARNING_SKILL.get(),
                                SoundSource.PLAYERS, 1.0F, 0.8F);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        // Чтение книги
        if (level.isClientSide()) {
            ClientHooks.openTomeScreen();
        } else {
            if (step == 1 && !player.getData(ModAttachments.TOME_OPENED)) {
                player.setData(ModAttachments.TOME_OPENED, true);
            } else if (step == 2 && !player.getData(ModAttachments.TOME_VISION_READ)) {
                player.setData(ModAttachments.TOME_VISION_READ, true);
            } else if (step == 11) {
                player.setData(ModAttachments.TOME_STEP, TomeLocator.SPHERE_STEP); // книга -> сфера
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---------- Поиск стихий ----------

    private static void searchElement(Level level, ServerPlayer player) {
        int found = 0;

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();

            if (fluid.is(FluidTags.WATER)) {
                found = WATER;
            } else if (fluid.is(FluidTags.LAVA)
                    || state.is(Blocks.MAGMA_BLOCK)) {
                found = FIRE;
            } else if (state.is(BlockTags.DIRT)
                    || state.is(Blocks.GRASS_BLOCK)) {
                found = EARTH;
            }
        } else if (isLookingAtOpenSky(level, player)) {
            found = AIR;
        }

        int mask = player.getData(ModAttachments.TOME_ELEMENTS);
        if (found == 0 || (mask & found) != 0) return; // ничего не подошло или стихия уже найдена

        mask |= found;
        player.setData(ModAttachments.TOME_ELEMENTS, mask);
        player.playNotifySound(DCSoundEvents.LEARNING_SKILL.get(), SoundSource.PLAYERS, 1.0F, 1.0F); // слышит только игрок

        if (mask == ALL_ELEMENTS) {
            player.setData(ModAttachments.TOME_LENS_ON, true);
            player.setData(ModAttachments.TOME_STEP, 4);
        }
    }

    /** Игрок смотрит вверх, и на 10 блоков вдоль взгляда нет ни одного блока. */
    private static boolean isLookingAtOpenSky(Level level, Player player) {
        if (player.getXRot() > SKY_MIN_PITCH) return false;

        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(SKY_CHECK_DISTANCE));
        BlockHitResult ray = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return ray.getType() == HitResult.Type.MISS;
    }
}