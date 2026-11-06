package ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Init.blocks.custom.multiblock.MultiblockHandler;
import ru.imaginaerum.damagecore.Init.blocks.custom.multiblock.RotatingMultiblock;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

import java.util.List;

public class ChestOfWildCurrentsBlock extends RotatingMultiblock implements EntityBlock {
    public static final MapCodec<ChestOfWildCurrentsBlock> CODEC = simpleCodec(ChestOfWildCurrentsBlock::new);

    // Размерность [height (1)][width (3)][depth (1)] для мультиблока 2x1x1
    public static final VoxelShape[][][] SHAPE = new VoxelShape[][][] {
            {
                    { Shapes.empty() },
                    {
                            box(0, 0, 0, 12, 18, 16)
                    },
                    {
                            box(4, 0, 0, 16, 18, 16)
                    }
            }
    };

    public ChestOfWildCurrentsBlock(Properties properties) {
        super(MultiblockHandler.MULTIBLOCK_2X1X1, createMultiblockShapeBuilder(MultiblockHandler.MULTIBLOCK_2X1X1, SHAPE), properties);
    }

    @Override
    protected MapCodec<? extends RotatingMultiblock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return multiblockHandler.isCenterState(state) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockPos centerPos = multiblockHandler.getCenterPos(pos, state, state.getValue(FACING));

        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(centerPos) instanceof ChestOfWildCurrentsEntity be)) return InteractionResult.PASS;

        if (!be.isOpened()) {
            // Проверяем наличие ключа в руках игрока
            boolean hasKey = player.getMainHandItem().is(DCItems.KEY_OF_WILD_CURRENTS.get()) ||
                    player.getOffhandItem().is(DCItems.KEY_OF_WILD_CURRENTS.get());

            if (!hasKey) {
                // Звук закрытого замка / щелчка механизма и сообщение
                level.playSound(null, centerPos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.8f, 0.6f);
                player.displayClientMessage(Component.translatable("block.damagecore.chest_of_wild_currents.locked"), true);
                return InteractionResult.CONSUME;
            }

            if (be.open(player)) {
                // Тратим ключ, если игрок не в творческом режиме
                if (!player.getAbilities().instabuild) {
                    if (player.getMainHandItem().is(DCItems.KEY_OF_WILD_CURRENTS.get())) {
                        player.getMainHandItem().shrink(1);
                    } else if (player.getOffhandItem().is(DCItems.KEY_OF_WILD_CURRENTS.get())) {
                        player.getOffhandItem().shrink(1);
                    }
                }

                // Звук открытия ванильного сундука (замените SoundEvents.CHEST_OPEN на ваш, когда будет готов)
                level.playSound(null, centerPos, DCSoundEvents.OPEN_CHEST.get(), SoundSource.BLOCKS, 0.8f, 0.9f);
            }
        } else if (be.isFinished()) {
            player.openMenu(be);
        }
        return InteractionResult.CONSUME;
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || multiblockHandler.isCenterState(state)) return;

        BlockPos center = multiblockHandler.getCenterPos(pos, state, state.getValue(FACING));
        if (level.getBlockEntity(center) instanceof ChestOfWildCurrentsEntity be) {
            be.applyComponents(stack.getComponents(), stack.getComponentsPatch());
            be.setChanged();
            BlockState cs = level.getBlockState(center);
            level.sendBlockUpdated(center, cs, cs, 3);
        }
    }
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!multiblockHandler.isCenterState(state)) {
            return List.of();
        }

        List<ItemStack> drops = super.getDrops(state, params);

        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof ChestOfWildCurrentsEntity be && be.wasOpened()) {

            for (ItemStack stack : drops) {
                if (stack.is(asItem())) {
                    CompoundTag tag = new CompoundTag();
                    tag.putBoolean("lockbox_opened", true);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        }

        return drops;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        BlockPos centerPos = multiblockHandler.getCenterPos(
                pos,
                state,
                state.getValue(FACING)
        );

        if (!level.isClientSide()) {
            if (level.getBlockEntity(centerPos) instanceof ChestOfWildCurrentsEntity be) {
                Containers.dropContents(level, centerPos, be);
                level.updateNeighbourForOutputSignal(centerPos, this);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return multiblockHandler.isCenterState(state) ? new ChestOfWildCurrentsEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ChestOfWildCurrentsEntity lockboxEntity) {
                ChestOfWildCurrentsEntity.serverTick(lvl, pos, st, lockboxEntity);
            }
        };
    }
}