package com.billtech.pipe;

import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidPipeTiers {
    private FluidPipeTiers() {
    }

    public static long maxRate(BlockState state) {
        return 1000;
    }

    public static int maxDistance(BlockState state) {
        return 64;
    }

    public static boolean allows(BlockState state, FluidVariant variant) {
        if (variant.getFluid() == ModFluids.METHANE) {
            return false;
        }
        return true;
    }
}
