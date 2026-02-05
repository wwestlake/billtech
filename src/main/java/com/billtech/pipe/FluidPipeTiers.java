package com.billtech.pipe;

import com.billtech.block.ModBlocks;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class FluidPipeTiers {
    private FluidPipeTiers() {
    }

    public static long maxRate(BlockState state) {
        if (state.is(ModBlocks.COPPER_WATER_PIPE)) {
            return 8100;
        }
        return 1000;
    }

    public static int maxDistance(BlockState state) {
        if (state.is(ModBlocks.COPPER_WATER_PIPE)) {
            return 32;
        }
        return 64;
    }

    public static boolean allows(BlockState state, FluidVariant variant) {
        if (variant.getFluid() == ModFluids.METHANE) {
            return false;
        }
        if (state.is(ModBlocks.COPPER_WATER_PIPE)) {
            return variant.getFluid() == Fluids.WATER || variant.getFluid() == Fluids.FLOWING_WATER;
        }
        return true;
    }
}
