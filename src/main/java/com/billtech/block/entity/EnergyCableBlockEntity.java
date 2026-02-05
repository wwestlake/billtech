package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.energy.EnergyCableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyCableBlockEntity extends BlockEntity {
    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity be) {
        EnergyCableNetwork.tick(level, pos);
    }
}
