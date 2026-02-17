package com.billtech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SentryCoverBlock extends Block {
    private static final VoxelShape PLATE = box(1, 12, 1, 15, 16, 15);
    private static final VoxelShape SPIRE = box(6, 4, 6, 10, 12, 10);
    private static final VoxelShape RING = box(3, 10, 3, 13, 12, 13);
    private static final VoxelShape SHAPE = Shapes.or(PLATE, SPIRE, RING);

    public SentryCoverBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
