package com.billtech.block;

import com.billtech.block.entity.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class PumpBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean connectEW = isPipeLike(level.getBlockState(pos.east())) || isPipeLike(level.getBlockState(pos.west()));
        boolean connectNS = isPipeLike(level.getBlockState(pos.north())) || isPipeLike(level.getBlockState(pos.south()));
        if (connectEW && !connectNS) {
            // Left/right flow should align with east/west neighbors.
            return this.defaultBlockState().setValue(FACING, Direction.NORTH);
        }
        if (connectNS && !connectEW) {
            // Left/right flow should align with north/south neighbors.
            return this.defaultBlockState().setValue(FACING, Direction.EAST);
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
            BlockState rotated = state.setValue(FACING, state.getValue(FACING).getClockWise());
            level.setBlock(pos, rotated, 3);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpBlockEntity(pos, state);
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
        return type == ModBlockEntities.PUMP
                ? (lvl, pos, st, be) -> PumpBlockEntity.serverTick(lvl, pos, st, (PumpBlockEntity) be)
                : null;
    }

    private static boolean isPipeLike(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock
                || state.getBlock() instanceof PumpBlock
                || state.getBlock() instanceof ValveBlock
                || state.getBlock() instanceof CheckValveBlock
                || state.getBlock() instanceof FlowMeterBlock
                || state.getBlock() instanceof RegulatorBlock;
    }
}
