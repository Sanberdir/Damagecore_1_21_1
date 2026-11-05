package ru.imaginaerum.damagecore.Init.blocks.custom.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RotatingMultiblock extends Block implements SimpleWaterloggedBlock, IDelegateProvider {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected final MultiblockHandler multiblockHandler;

    protected final Map<BlockState, VoxelShape> blockShapes = new HashMap<>();
    protected final Map<BlockState, VoxelShape> multiblockShapes = new HashMap<>();

    private final ShapeBuilder shapeBuilder;

    // Потокобезопасный/статичный временный хендлер для передачи в super() конструктора
    private static MultiblockHandler CURRENT_BUILDING_HANDLER = null;

    protected RotatingMultiblock(MultiblockHandler multiblockHandler,
                                 ShapeBuilder shapeBuilder,
                                 Properties pProperties) {
        super(prepareProperties(multiblockHandler, pProperties));
        this.multiblockHandler = multiblockHandler;
        this.shapeBuilder = shapeBuilder;

        this.registerDefaultState(this.multiblockHandler.getCenterState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)));

        this.precalculateShapes();
    }

    private static Properties prepareProperties(MultiblockHandler handler, Properties properties) {
        CURRENT_BUILDING_HANDLER = handler;
        return properties.dynamicShape();
    }

    public MultiblockHandler getMultiblockHandler() {
        return multiblockHandler;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED, FACING);
        // Используем переданный на этапе вызова super() хендлер, так как поле еще не инициализировано
        if (multiblockHandler != null) {
            multiblockHandler.createBlockStateDefinition(pBuilder);
        } else if (CURRENT_BUILDING_HANDLER != null) {
            CURRENT_BUILDING_HANDLER.createBlockStateDefinition(pBuilder);
        }
    }

    @Override
    public BlockPos getDelegatePos(BlockState blockState, BlockPos blockPos) {
        return multiblockHandler.getCenterPos(blockPos, blockState, blockState.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        final Direction direction = pContext.getHorizontalDirection().getOpposite();
        final boolean waterlogged = pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER;
        final BlockState blockState = this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(WATERLOGGED, waterlogged);
        return multiblockHandler.getStateForPlacement(pContext, blockState, direction);
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        if (!multiblockHandler.canSurvive(pState, pLevel, pCurrentPos, pState.getValue(FACING))) {
            return Fluids.WATER.getSource(false).is(pLevel.getFluidState(pCurrentPos).getType()) ?
                    pLevel.getFluidState(pCurrentPos).createLegacyBlock() : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        multiblockHandler.onBlockPlaced(pLevel, pPos, pState, pState.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        if (!pLevel.getBlockState(pPos).is(this)) {
            return true;
        }
        return multiblockHandler.canSurvive(pState, pLevel, pPos, pState.getValue(FACING));
    }

    @Override
    public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pLevel.isClientSide() && pPlayer.isCreative()) {
            multiblockHandler.preventCreativeDropFromCenterPart(pLevel, pPos, pState, pState.getValue(FACING), pPlayer);
        }
        return super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState pState) {
        return PushReaction.BLOCK;
    }

    public void removeAll(final Level level, final BlockPos centerPos) {
        final BlockPos.MutableBlockPos mutablePos = centerPos.mutable();
        getMultiblockHandler().iterateIndices(index -> {
            level.removeBlock(mutablePos.setWithOffset(centerPos, index), false);
        });
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.hasCollision ? getBlockShape(pState) : Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getMultiblockShape(pState);
    }

    protected void precalculateShapes() {
        blockShapes.clear();
        multiblockShapes.clear();
        final Map<Direction, VoxelShape> centeredVisualShapes = new EnumMap<>(Direction.class);
        centeredVisualShapes.putAll(ShapeUtils.rotateShapes(MultiblockHandler.ORIGIN_DIRECTION, createMultiblockShape()));

        for (BlockState blockState : this.stateDefinition.getPossibleStates()) {
            blockShapes.put(blockState, this.shapeBuilder.apply(blockState));
            Direction direction = blockState.getValue(FACING);
            Vec3i index = multiblockHandler.getIndex(blockState);
            Vec3i offset = MultiblockHandler.indexToOffset(index, direction);
            VoxelShape shape = centeredVisualShapes.get(blockState.getValue(FACING))
                    .move(-offset.getX(), -offset.getY(), -offset.getZ());
            multiblockShapes.put(blockState, shape);
        }
    }

    protected VoxelShape createMultiblockShape() {
        final BlockState blockState = multiblockHandler.getCenterState(defaultBlockState());
        final AtomicReference<VoxelShape> shape = new AtomicReference<>(Shapes.empty());
        multiblockHandler.iterateIndices(index -> {
            BlockState b = multiblockHandler.getIndexedState(blockState, index);
            shape.set(ShapeUtils.orUnoptimized(shape.get(), blockShapes.computeIfAbsent(b, this.shapeBuilder)
                    .move(-index.getX(), index.getY(), index.getZ())
            ));
        });
        return shape.get().optimize();
    }

    public VoxelShape getBlockShape(final BlockState blockState) {
        return blockShapes.get(blockState);
    }

    public VoxelShape getMultiblockShape(final BlockState blockState) {
        return multiblockShapes.get(blockState);
    }

    //// SHAPE HELPER METHODS ////

    public static ShapeBuilder createMultiblockShapeBuilder(final MultiblockHandler handler, final VoxelShape[][][] template) {
        return blockState -> {
            final Vec3i index = handler.getIndex(blockState);
            final Vec3i dimensions = handler.getDimensions();
            final Direction facing = blockState.getValue(FACING);
            int heightIndex = (index.getY() + dimensions.getY() / 2);
            int widthIndex = (index.getX() + dimensions.getX() / 2);
            int depthIndex = (index.getZ() + dimensions.getZ() / 2);
            final VoxelShape shape = template[heightIndex][widthIndex][depthIndex];
            return ShapeUtils.rotateShape(MultiblockHandler.ORIGIN_DIRECTION, facing, shape);
        };
    }
}