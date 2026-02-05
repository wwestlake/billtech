package com.billtech.energy;

import com.billtech.block.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;

public final class EnergyCableTiers {
    private EnergyCableTiers() {
    }

    public static long maxRate(BlockState state) {
        if (state.is(ModBlocks.CLOTH_INSULATED_COPPER_CABLE)) {
            return 100;
        }
        if (state.is(ModBlocks.INSULATED_COPPER_CABLE)) {
            return 250;
        }
        return 250;
    }

    public static int maxDistance(BlockState state) {
        if (state.is(ModBlocks.CLOTH_INSULATED_COPPER_CABLE)) {
            return 16;
        }
        if (state.is(ModBlocks.INSULATED_COPPER_CABLE)) {
            return 32;
        }
        return 32;
    }
}
