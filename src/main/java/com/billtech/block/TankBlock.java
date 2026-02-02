package com.billtech.block;

import com.billtech.block.entity.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TankBlock extends Block implements EntityBlock {
    public TankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.TANK_BLOCK
                ? (lvl, pos, st, be) -> TankBlockEntity.serverTick(lvl, pos, st, (TankBlockEntity) be)
                : null;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof TankBlock) {
            return true;
        }
        return super.skipRendering(state, neighborState, direction);
    }
}
