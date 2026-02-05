package com.billtech.pipe;

import com.billtech.block.ModBlocks;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.block.state.BlockState;

public final class GasPipeTiers {
    private GasPipeTiers() {
    }

    public static long maxRate(BlockState state) {
        if (state.is(ModBlocks.GAS_PIPE)) {
            return 1000;
        }
        return 1000;
    }

    public static int maxDistance(BlockState state) {
        if (state.is(ModBlocks.GAS_PIPE)) {
            return 64;
        }
        return 64;
    }

    public static boolean allows(BlockState state, FluidVariant variant) {
        return variant != null && variant.getFluid() == ModFluids.METHANE;
    }
}
