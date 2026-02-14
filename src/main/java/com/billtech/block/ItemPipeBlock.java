package com.billtech.block;

import com.billtech.BillTech;
import com.billtech.block.entity.ItemPipeBlockEntity;
import com.billtech.block.AutoCrafterBlock;
import com.billtech.cover.CoverInteraction;
import com.billtech.cover.CoverProvider;
import com.billtech.stripe.StripeCarrier;
import com.billtech.stripe.StripeItemData;
import com.billtech.stripe.StripeUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.Container;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ItemPipeBlock extends Block implements EntityBlock {
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

    public ItemPipeBlock(Properties properties) {
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
        return new ItemPipeBlockEntity(pos, state);
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
        return type == ModBlockEntities.ITEM_PIPE
                ? (lvl, pos, st, be) -> ItemPipeBlockEntity.serverTick(lvl, pos, st, (ItemPipeBlockEntity) be)
                : null;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StripeCarrier carrier) {
            carrier.setStripeData(StripeItemData.read(stack));
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoverProvider coverable) {
            InteractionResult result = CoverInteraction.handle(level, player, ItemStack.EMPTY, hit.getDirection(), coverable);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoverProvider coverable) {
            InteractionResult result = CoverInteraction.handle(level, player, stack, hit.getDirection(), coverable);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
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
    protected VoxelShape getVisualShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
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

    public BlockState updateConnections(LevelReader level, BlockPos pos, BlockState state) {
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
        if (neighborState.getBlock() instanceof ItemPipeBlock) {
            return StripeUtil.canConnect(level, pos, neighborPos) ? ConnectionType.PIPE : ConnectionType.NONE;
        }
        if (neighborState.getBlock() instanceof ItemControllerBlock
                || neighborState.getBlock() instanceof AutoCrafterBlock) {
            return ConnectionType.ENDPOINT;
        }
        if (neighborState.getBlock() instanceof FluidPipeBlock) {
            return ConnectionType.NONE;
        }
        if (level instanceof Level world) {
            BlockEntity be = world.getBlockEntity(neighborPos);
            if (be instanceof Container) {
                logConnection(world, pos, dir, neighborPos, be, "container");
                return ConnectionType.ENDPOINT;
            }
            // Probe both the opposite face (touching face) and the same face to handle sided/non-sided inventories.
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, neighborPos, dir.getOpposite());
            if (storage == null) {
                storage = ItemStorage.SIDED.find(world, neighborPos, dir);
            }
            if (storage != null) {
                logConnection(world, pos, dir, neighborPos, be, "storage");
                return ConnectionType.ENDPOINT;
            }
        }
        return ConnectionType.NONE;
    }

    private void logConnection(Level world, BlockPos pos, Direction dir, BlockPos neighborPos, BlockEntity be, String kind) {
        if (world != null && world.isClientSide) {
            return;
        }
        String msg = String.format(
                "ItemPipe debug: %s pipe=%s dir=%s neighbor=%s be=%s",
                kind,
                pos.toShortString(),
                dir,
                neighborPos.toShortString(),
                be == null ? "null" : be.getType().toString()
        );
        BillTech.LOGGER.info(msg);
    }
}
