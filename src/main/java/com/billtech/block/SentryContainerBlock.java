package com.billtech.block;

import com.billtech.block.entity.SentryContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SentryContainerBlock extends Block implements EntityBlock {
    private static final VoxelShape FLOOR = box(1, 0, 1, 15, 2, 15);
    private static final VoxelShape CEILING = box(1, 14, 1, 15, 16, 15);
    private static final VoxelShape WEST_COLUMN = box(1, 2, 1, 3, 14, 3);
    private static final VoxelShape NORTH_COLUMN = box(13, 2, 1, 15, 14, 3);
    private static final VoxelShape EAST_COLUMN = box(1, 2, 13, 3, 14, 15);
    private static final VoxelShape SOUTH_COLUMN = box(13, 2, 13, 15, 14, 15);
    private static final VoxelShape SHAPE = Shapes.or(
            FLOOR,
            CEILING,
            WEST_COLUMN,
            NORTH_COLUMN,
            EAST_COLUMN,
            SOUTH_COLUMN
    );

    public SentryContainerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SentryContainerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.SENTRY_CONTAINER
                ? (lvl, pos, st, be) -> SentryContainerBlockEntity.serverTick(lvl, pos, st, (SentryContainerBlockEntity) be)
                : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
