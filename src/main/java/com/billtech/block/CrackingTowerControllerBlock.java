package com.billtech.block;

import com.billtech.block.entity.CrackingTowerControllerBlockEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CrackingTowerControllerBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CrackingTowerControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrackingTowerControllerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.CRACKING_TOWER_CONTROLLER
                ? (lvl, pos, st, be) -> CrackingTowerControllerBlockEntity.serverTick(lvl, pos, st, (CrackingTowerControllerBlockEntity) be)
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
        if (be instanceof CrackingTowerControllerBlockEntity) {
            player.openMenu((MenuProvider) be);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updateAssembly(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        updateAssembly(level, pos);
    }

    public static void updateAssembly(Level level, BlockPos controllerPos) {
        if (level.isClientSide) {
            return;
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof CrackingTowerControllerBlock)) {
            return;
        }
        Direction facing = controllerState.getValue(FACING);
        Direction back = facing.getOpposite();
        BlockPos basePos = controllerPos.relative(back);
        boolean complete = true;
        for (int i = 0; i < 4; i++) {
            BlockPos segmentPos = basePos.above(i);
            BlockState segmentState = level.getBlockState(segmentPos);
            if (!(segmentState.getBlock() instanceof CrackingTowerBlock)) {
                complete = false;
                break;
            }
        }
        for (int i = 0; i < 4; i++) {
            BlockPos segmentPos = basePos.above(i);
            BlockState segmentState = level.getBlockState(segmentPos);
            if (!(segmentState.getBlock() instanceof CrackingTowerBlock)) {
                continue;
            }
            BlockState updated = segmentState
                    .setValue(CrackingTowerBlock.FACING, facing)
                    .setValue(CrackingTowerBlock.ASSEMBLED, complete)
                    .setValue(CrackingTowerBlock.SEGMENT, complete ? i : 0);
            if (updated != segmentState) {
                level.setBlock(segmentPos, updated, 3);
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
        builder.add(FACING);
    }
}
