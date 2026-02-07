package com.billtech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class CrackingTowerBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 3);

    public CrackingTowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ASSEMBLED, false)
                .setValue(SEGMENT, 0));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ASSEMBLED, false)
                .setValue(SEGMENT, 0);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        notifyController(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        notifyController(level, pos);
        if (level.isClientSide) {
            return;
        }
        if (!hasControllerNearby(level, pos)) {
            if (state.getValue(ASSEMBLED)) {
                level.setBlock(pos, state.setValue(ASSEMBLED, false).setValue(SEGMENT, 0), 3);
            }
        }
    }

    private void notifyController(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockPos controllerPos = findController(level, pos);
        if (controllerPos != null) {
            CrackingTowerControllerBlock.updateAssembly(level, controllerPos);
        }
    }

    private boolean hasControllerNearby(Level level, BlockPos pos) {
        return findController(level, pos) != null;
    }

    private BlockPos findController(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int depth = 0; depth < 4; depth++) {
                BlockPos candidate = pos.below(depth).relative(dir);
                if (level.getBlockState(candidate).getBlock() instanceof CrackingTowerControllerBlock) {
                    return candidate;
                }
            }
        }
        return null;
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
        builder.add(FACING, ASSEMBLED, SEGMENT);
    }
}
