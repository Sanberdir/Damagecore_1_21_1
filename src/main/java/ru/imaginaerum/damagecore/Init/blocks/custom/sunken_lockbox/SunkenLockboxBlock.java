package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Init.blocks.entity.DCBlockEntities;

import java.util.List;

public class SunkenLockboxBlock extends BaseEntityBlock {
    public static final MapCodec<SunkenLockboxBlock> CODEC = simpleCodec(SunkenLockboxBlock::new);
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final int ROTATIONS = RotationSegment.getMaxSegmentIndex() + 1;

    // симметрично по X и Z, чтобы хитбокс подходил при любом повороте
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 8, 14);

    public SunkenLockboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SunkenLockboxEntity be)) return InteractionResult.PASS;

        if (!be.isOpened()) {
            if (be.open(player)) {
                level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 0.9f);
            }
        } else if (be.isFinished()) {
            player.openMenu(be);   // обычный GUI на 27 слотов
        }
        return InteractionResult.CONSUME;
    }
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SunkenLockboxEntity be
                && be.isFinished()) {
            for (ItemStack s : drops) {
                if (s.is(asItem())) {
                    CompoundTag t = new CompoundTag();
                    t.putBoolean("lockbox_finished", true);
                    s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
                }
            }
        }
        return drops;
    }

    // синхронизация клиента после установки предмета с меткой
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SunkenLockboxEntity be && be.isFinished()) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    // содержимое выпадает при поломке
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SunkenLockboxEntity be) {
                Containers.dropContents(level, pos, be);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()));
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, DCBlockEntities.SUNKEN_LOCKBOX.get(), SunkenLockboxEntity::serverTick);
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(ROTATION, rot.rotate(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        // только если сверху у блока снизу полная грань, то есть он целый кубом
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState(); // блок под сундуком убрали, сундук ломается
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SunkenLockboxEntity(pos, state);
    }
}