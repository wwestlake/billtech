package com.billtech.block;

import com.billtech.block.entity.FlowMeterBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FlowMeterBlock extends Block implements EntityBlock {
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.create("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.create("down", ConnectionType.class);
    public static final EnumProperty<Direction> DISPLAY = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");
    private static final VoxelShape CENTER_SHAPE = box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_SHAPE = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape EAST_SHAPE = box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_SHAPE = box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape UP_SHAPE = box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = box(6, 0, 6, 10, 6, 10);

    public FlowMeterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, ConnectionType.NONE)
                .setValue(SOUTH, ConnectionType.NONE)
                .setValue(EAST, ConnectionType.NONE)
                .setValue(WEST, ConnectionType.NONE)
                .setValue(UP, ConnectionType.NONE)
                .setValue(DOWN, ConnectionType.NONE)
                .setValue(DISPLAY, Direction.NORTH)
                .setValue(REVERSED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlowMeterBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.FLOW_METER
                ? (lvl, pos, st, be) -> FlowMeterBlockEntity.serverTick(lvl, pos, st, (FlowMeterBlockEntity) be)
                : null;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
        return state.setValue(DISPLAY, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess tickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        return updateConnections(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, DISPLAY, REVERSED);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    private VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CENTER_SHAPE;
        if (state.getValue(NORTH) != ConnectionType.NONE) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (state.getValue(SOUTH) != ConnectionType.NONE) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (state.getValue(EAST) != ConnectionType.NONE) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (state.getValue(WEST) != ConnectionType.NONE) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (state.getValue(UP) != ConnectionType.NONE) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (state.getValue(DOWN) != ConnectionType.NONE) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }
        return shape;
    }

    private BlockState updateConnections(LevelReader level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, connectionType(level, pos, Direction.NORTH))
                .setValue(SOUTH, connectionType(level, pos, Direction.SOUTH))
                .setValue(EAST, connectionType(level, pos, Direction.EAST))
                .setValue(WEST, connectionType(level, pos, Direction.WEST))
                .setValue(UP, connectionType(level, pos, Direction.UP))
                .setValue(DOWN, connectionType(level, pos, Direction.DOWN));
    }

    private ConnectionType connectionType(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof FluidPipeBlock) {
            return ConnectionType.PIPE;
        }
        if (neighborState.getBlock() instanceof PumpBlock
                || neighborState.getBlock() instanceof ValveBlock
                || neighborState.getBlock() instanceof CheckValveBlock
                || neighborState.getBlock() instanceof FlowMeterBlock
                || neighborState.getBlock() instanceof RegulatorBlock) {
            return ConnectionType.PIPE;
        }
        if (level instanceof Level world) {
            Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, neighborPos, dir.getOpposite());
            return storage != null ? ConnectionType.ENDPOINT : ConnectionType.NONE;
        }
        return ConnectionType.NONE;
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            boolean reversed = state.getValue(REVERSED);
            level.setBlock(pos, state.setValue(REVERSED, !reversed), 3);
            return InteractionResult.CONSUME;
        }
        Direction next = state.getValue(DISPLAY).getClockWise();
        level.setBlock(pos, state.setValue(DISPLAY, next), 3);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FlowMeterBlockEntity meter) {
            return meter.getRedstoneLevel();
        }
        return 0;
    }
}
