package ru.imaginaerum.damagecore.Init.blocks.custom.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MultiblockHandler {

    public static final Direction ORIGIN_DIRECTION = Direction.NORTH;
    public static final Vec3i CENTER_INDEX = Vec3i.ZERO;

    protected static final String WIDTH = "width";
    protected static final String HEIGHT = "height";
    protected static final String DEPTH = "depth";

    // Свойства для разных размеров мультиблоков
    protected static final IntegerProperty WIDTH_0_1 = IntegerProperty.create(WIDTH, 0, 1);
    protected static final IntegerProperty WIDTH_1_2 = IntegerProperty.create(WIDTH, 1, 2);
    protected static final IntegerProperty WIDTH_0_2 = IntegerProperty.create(WIDTH, 0, 2);

    protected static final IntegerProperty HEIGHT_1_2 = IntegerProperty.create(HEIGHT, 1, 2);
    protected static final IntegerProperty HEIGHT_0_2 = IntegerProperty.create(HEIGHT, 0, 2);

    protected static final IntegerProperty DEPTH_0_1 = IntegerProperty.create(DEPTH, 0, 1);
    protected static final IntegerProperty DEPTH_1_2 = IntegerProperty.create(DEPTH, 1, 2);
    protected static final IntegerProperty DEPTH_0_2 = IntegerProperty.create(DEPTH, 0, 2);

    // Массивы маппинга по максимальному значению (размеру)
    protected static final IntegerProperty[] WIDTH_BY_MAX_VALUE = new IntegerProperty[]{WIDTH_0_1, WIDTH_1_2, WIDTH_0_2};
    protected static final IntegerProperty[] HEIGHT_BY_MAX_VALUE = new IntegerProperty[]{null, HEIGHT_1_2, HEIGHT_0_2};
    protected static final IntegerProperty[] DEPTH_BY_MAX_VALUE = new IntegerProperty[]{DEPTH_0_1, DEPTH_1_2, DEPTH_0_2};

    // Готовые константы мультиблоков
    public static final MultiblockHandler MULTIBLOCK_2X1X1 = new MultiblockHandler(2, 1, 1);
    public static final MultiblockHandler MULTIBLOCK_2X1X2 = new MultiblockHandler(2, 1, 2);
    public static final MultiblockHandler MULTIBLOCK_2X2X2 = new MultiblockHandler(2, 2, 2);
    // ... другие константы можно добавить по необходимости

    protected final @Nullable IntegerProperty widthProperty;
    protected final @Nullable IntegerProperty heightProperty;
    protected final @Nullable IntegerProperty depthProperty;

    protected final Vec3i dimensions;
    protected final Vec3i minIndex;
    protected final Vec3i maxIndex;
    protected final Map<Direction, BoundingBox> bounds;

    public MultiblockHandler(final int width, final int height, final int depth) {
        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("Dimensions must be > 0");
        }
        this.dimensions = new Vec3i(width, height, depth);
        this.minIndex = new Vec3i(-(width - 1) / 2, -(height - 1) / 2, -(depth - 1) / 2);
        this.maxIndex = new Vec3i(width / 2, height / 2, depth / 2);
        this.widthProperty = getWidthProperty(width);
        this.heightProperty = getHeightProperty(height);
        this.depthProperty = getDepthProperty(depth);
        final BoundingBox boundingBox = BoundingBox.fromCorners(minIndex, maxIndex);
        this.bounds = createRotatedBoundingBoxMap(boundingBox, ORIGIN_DIRECTION);
    }

    public @Nullable IntegerProperty getWidthProperty() { return widthProperty; }
    public @Nullable IntegerProperty getHeightProperty() { return heightProperty; }
    public @Nullable IntegerProperty getDepthProperty() { return depthProperty; }

    public Vec3i getDimensions() { return new Vec3i(dimensions.getX(), dimensions.getY(), dimensions.getZ()); }
    public Vec3i getMinIndex() { return new Vec3i(minIndex.getX(), minIndex.getY(), minIndex.getZ()); }
    public Vec3i getMaxIndex() { return new Vec3i(maxIndex.getX(), maxIndex.getY(), maxIndex.getZ()); }
    public BoundingBox getBounds(final Direction direction) { return bounds.get(direction); }

    public BlockPos getCenterPos(final BlockPos pos, final BlockState blockState, final Direction direction) {
        return getCenterPos(pos, getIndex(blockState), direction);
    }

    public BlockPos getMin(final BlockPos center, final Direction direction) {
        return center.offset(MultiblockHandler.indexToOffset(minIndex, direction));
    }

    public BlockPos getMax(final BlockPos center, final Direction direction) {
        return center.offset(MultiblockHandler.indexToOffset(maxIndex, direction));
    }

    public Iterable<BlockPos> getPositions(final BlockPos center, final Direction facing) {
        return BlockPos.betweenClosed(getMin(center, facing), getMax(center, facing));
    }

    public static @Nullable IntegerProperty getWidthProperty(final int maxWidth) {
        if (maxWidth <= 1) return null;

        final int index = Mth.clamp(maxWidth - 1, 0, WIDTH_BY_MAX_VALUE.length - 1);
        return WIDTH_BY_MAX_VALUE[index];
    }

    public static @Nullable IntegerProperty getHeightProperty(final int maxHeight) {
        final int index = Mth.clamp(maxHeight - 1, 0, HEIGHT_BY_MAX_VALUE.length - 1);
        return HEIGHT_BY_MAX_VALUE[index];
    }

    public static @Nullable IntegerProperty getDepthProperty(final int maxDepth) {
        if (maxDepth <= 1) return null;

        final int index = Mth.clamp(maxDepth - 1, 0, DEPTH_BY_MAX_VALUE.length - 1);
        return DEPTH_BY_MAX_VALUE[index];
    }

    public boolean isCenterState(final BlockState blockState) {
        return getIndex(blockState).equals(CENTER_INDEX);
    }

    public StateDefinition.Builder<Block, BlockState> createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (widthProperty != null) builder.add(widthProperty);
        if (heightProperty != null) builder.add(heightProperty);
        if (depthProperty != null) builder.add(depthProperty);
        return builder;
    }

    public BlockState getCenterState(BlockState blockState) {
        return getIndexedState(blockState, CENTER_INDEX);
    }

    public Vec3i getIndex(final BlockState blockState) {
        final int width = widthProperty != null ? (blockState.getValue(widthProperty) - dimensions.getX() / 2) : 0;
        final int height = heightProperty != null ? (blockState.getValue(heightProperty) - dimensions.getY() / 2) : 0;
        final int depth = depthProperty != null ? (blockState.getValue(depthProperty) - dimensions.getZ() / 2) : 0;
        return new Vec3i(width, height, depth);
    }

    public BlockState getIndexedState(final BlockState blockState, final Vec3i index) {
        BlockState mutableBlockState = blockState;
        if (widthProperty != null) mutableBlockState = mutableBlockState.setValue(widthProperty, index.getX() + dimensions.getX() / 2);
        if (heightProperty != null) mutableBlockState = mutableBlockState.setValue(heightProperty, index.getY() + dimensions.getY() / 2);
        if (depthProperty != null) mutableBlockState = mutableBlockState.setValue(depthProperty, index.getZ() + dimensions.getZ() / 2);
        return mutableBlockState;
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context, BlockState blockState, Direction facing) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();

        // 1) кликнутая клетка = главная часть (как раньше)
        if (fits(context, clicked, facing)) {
            return getCenterState(blockState);
        }

        // 2) кликнутая клетка = любая другая часть, центр сдвигается на её индекс
        final BlockState[] result = {null};
        iterateIndices(index -> {
            if (result[0] != null || index.equals(CENTER_INDEX)) return;
            Vec3i idx = new Vec3i(index.getX(), index.getY(), index.getZ()); // index - переиспользуемый mutable
            BlockPos center = getCenterPos(clicked, idx, facing);
            if (fits(context, center, facing)) {
                result[0] = getIndexedState(blockState, idx);
            }
        });
        return result[0];
    }

    private boolean fits(BlockPlaceContext context, BlockPos center, Direction facing) {
        final Level level = context.getLevel();
        return allPositions(center, facing, p -> level.isInWorldBounds(p) && level.getBlockState(p).canBeReplaced(context));
    }

    public void onBlockPlaced(Level level, BlockPos pos, BlockState blockState, Direction direction) {
        final Vec3i placedIndex = getIndex(blockState);
        final BlockPos center = getCenterPos(pos, placedIndex, direction);
        iterateIndices(index -> {
            if (index.equals(placedIndex)) return;   // эту часть уже поставил предмет
            BlockPos p = center.offset(indexToOffset(index, direction));
            boolean waterlogged = level.getFluidState(p).getType() == Fluids.WATER;
            final BlockState state = getIndexedState(blockState.setValue(BlockStateProperties.WATERLOGGED, waterlogged), index);
            level.setBlock(p, state, Block.UPDATE_ALL);
        });
    }

    public boolean canSurvive(final BlockState blockState, final LevelReader level, final BlockPos pos, final Direction facing) {
        return allPositions(getCenterPos(pos, blockState, facing), facing, p -> level.getBlockState(p).is(blockState.getBlock()));
    }

    public void preventCreativeDropFromCenterPart(Level level, BlockPos pos, BlockState blockState, Direction facing, Player player) {
        final BlockPos origin = getCenterPos(pos, blockState, facing);
        final BlockState originState = level.getBlockState(origin);
        if (originState.is(blockState.getBlock()) && getIndex(originState).equals(CENTER_INDEX)) {
            level.setBlock(origin, originState.getFluidState().createLegacyBlock(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
        }
    }

    public boolean allPositions(final BlockPos center, final Direction facing, final Predicate<BlockPos> predicate) {
        for (BlockPos p : getPositions(center, facing)) {
            if (!predicate.test(p)) return false;
        }
        return true;
    }

    public void iterateIndices(final Consumer<Vec3i> consumer) {
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = minIndex.getX(); x <= maxIndex.getX(); x++) {
            for (int y = minIndex.getY(); y <= maxIndex.getY(); y++) {
                for (int z = minIndex.getZ(); z <= maxIndex.getZ(); z++) {
                    consumer.accept(mutable.set(x, y, z));
                }
            }
        }
    }

    protected static Map<Direction, BoundingBox> createRotatedBoundingBoxMap(final BoundingBox boundingBox, final Direction from) {
        final Map<Direction, BoundingBox> map = new EnumMap<>(Direction.class);
        map.put(from, boundingBox);
        BoundingBox box = boundingBox;
        for (int i = 0; i < 3; i++) {
            Direction direction = Direction.from2DDataValue(from.get2DDataValue() + i + 1);
            box = new BoundingBox(box.minZ(), box.minY(), box.minX(), box.maxZ(), box.maxY(), box.maxX());
            map.put(direction, box);
        }
        return map;
    }

    public static BlockPos getCenterPos(final BlockPos pos, final Vec3i index, final Direction direction) {
        return pos.subtract(indexToOffset(index, direction));
    }

    public static Vec3i indexToOffset(final Vec3i index, final Direction direction) {
        return switch (direction) {
            case EAST -> new Vec3i(-index.getZ(), index.getY(), -index.getX());
            case SOUTH -> new Vec3i(index.getX(), index.getY(), -index.getZ());
            case WEST -> new Vec3i(index.getZ(), index.getY(), index.getX());
            default -> new Vec3i(-index.getX(), index.getY(), index.getZ());
        };
    }
}