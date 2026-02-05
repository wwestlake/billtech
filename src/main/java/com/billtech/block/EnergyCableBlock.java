package com.billtech.block;

import com.billtech.block.entity.EnergyCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

public class EnergyCableBlock extends Block implements EntityBlock {
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.create("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.create("down", ConnectionType.class);
    private static final VoxelShape CENTER_SHAPE = box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_SHAPE = box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape EAST_SHAPE = box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_SHAPE = box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape UP_SHAPE = box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = box(6, 0, 6, 10, 6, 10);

    public EnergyCableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, ConnectionType.NONE)
                .setValue(SOUTH, ConnectionType.NONE)
                .setValue(EAST, ConnectionType.NONE)
                .setValue(WEST, ConnectionType.NONE)
                .setValue(UP, ConnectionType.NONE)
                .setValue(DOWN, ConnectionType.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
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
        return type == ModBlockEntities.ENERGY_CABLE
                ? (lvl, pos, st, be) -> EnergyCableBlockEntity.serverTick(lvl, pos, st, (EnergyCableBlockEntity) be)
                : null;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
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
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
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
        if (neighborState.getBlock() instanceof EnergyCableBlock) {
            return ConnectionType.PIPE;
        }
        if (level instanceof Level world) {
            team.reborn.energy.api.EnergyStorage storage =
                    team.reborn.energy.api.EnergyStorage.SIDED.find(world, neighborPos, dir.getOpposite());
            return storage != null ? ConnectionType.ENDPOINT : ConnectionType.NONE;
        }
        return ConnectionType.NONE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (level.random.nextFloat() < 0.1f) {
            living.hurt(level.damageSources().lightningBolt(), 2.0f);
        }
    }
}
