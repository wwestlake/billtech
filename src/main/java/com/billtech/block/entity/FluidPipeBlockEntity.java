package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.billtech.pipe.FluidPipeNetwork;

public class FluidPipeBlockEntity extends BlockEntity {
    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        FluidPipeNetwork.tick(level, pos);
    }
}
