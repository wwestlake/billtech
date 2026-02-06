package com.billtech.block;

import com.billtech.block.entity.SeparatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SeparatorBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    public SeparatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ASSEMBLED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeparatorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.SEPARATOR
                ? (lvl, pos, st, be) -> SeparatorBlockEntity.serverTick(lvl, pos, st, (SeparatorBlockEntity) be)
                : null;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                       net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SeparatorBlockEntity) {
            player.openMenu((MenuProvider) be);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        if (above.getBlock() instanceof GrinderBlock) {
            facing = above.getValue(GrinderBlock.FACING);
        }
        return defaultBlockState().setValue(FACING, facing).setValue(ASSEMBLED, false);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updateAssembly(level, pos);
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        updateAssembly(level, pos);
    }

    public static void updateAssembly(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GrinderBlock) {
            updateGrinder(level, pos, state);
        } else if (state.getBlock() instanceof SeparatorBlock) {
            updateSeparator(level, pos, state);
        }
    }

    private static void updateGrinder(Level level, BlockPos pos, BlockState state) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        boolean assembled = below.getBlock() instanceof SeparatorBlock;
        if (state.getValue(GrinderBlock.ASSEMBLED) != assembled) {
            level.setBlock(pos, state.setValue(GrinderBlock.ASSEMBLED, assembled), 3);
        }
        if (assembled) {
            Direction target = below.getValue(SeparatorBlock.FACING);
            BlockState updated = level.getBlockState(pos);
            if (updated.getValue(GrinderBlock.FACING) != target) {
                level.setBlock(pos, updated.setValue(GrinderBlock.FACING, target).setValue(GrinderBlock.ASSEMBLED, true), 3);
            }
            if (!below.getValue(SeparatorBlock.ASSEMBLED)) {
                level.setBlock(belowPos, below.setValue(SeparatorBlock.ASSEMBLED, true), 3);
            }
        }
    }

    private static void updateSeparator(Level level, BlockPos pos, BlockState state) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        boolean assembled = above.getBlock() instanceof GrinderBlock;
        if (state.getValue(SeparatorBlock.ASSEMBLED) != assembled) {
            level.setBlock(pos, state.setValue(SeparatorBlock.ASSEMBLED, assembled), 3);
        }
        if (assembled) {
            Direction target = state.getValue(SeparatorBlock.FACING);
            if (above.getValue(GrinderBlock.FACING) != target) {
                level.setBlock(abovePos, above.setValue(GrinderBlock.FACING, target).setValue(GrinderBlock.ASSEMBLED, true), 3);
            } else if (!above.getValue(GrinderBlock.ASSEMBLED)) {
                level.setBlock(abovePos, above.setValue(GrinderBlock.ASSEMBLED, true), 3);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ASSEMBLED);
    }
}
