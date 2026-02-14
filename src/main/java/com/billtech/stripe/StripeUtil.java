package com.billtech.stripe;

import com.billtech.block.EnergyCableBlock;
import com.billtech.block.FluidPipeBlock;
import com.billtech.block.GasPipeBlock;
import com.billtech.block.ItemPipeBlock;
import com.billtech.pipe.ItemPipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class StripeUtil {
    private StripeUtil() {
    }

    public static boolean canConnect(LevelReader level, BlockPos a, BlockPos b) {
        BlockEntity beA = level.getBlockEntity(a);
        BlockEntity beB = level.getBlockEntity(b);
        boolean aStripeBlock = isStripeBlock(level, a);
        boolean bStripeBlock = isStripeBlock(level, b);
        if (aStripeBlock && !(beA instanceof StripeCarrier)) {
            return false; // stripe-capable but BE missing/unready
        }
        if (bStripeBlock && !(beB instanceof StripeCarrier)) {
            return false;
        }
        if (!aStripeBlock || !bStripeBlock) {
            return true; // Only gate when both are stripe-capable.
        }
        StripeData dataA = ((StripeCarrier) beA).getStripeData();
        StripeData dataB = ((StripeCarrier) beB).getStripeData();
        if (dataA == null || dataB == null) {
            return false; // stripe-capable but missing data -> safest to not connect.
        }
        return dataA.signature() == dataB.signature();
    }

    public static void notifyStripeChanged(Level level, BlockPos pos) {
        refreshConnections(level, pos);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        level.setBlocksDirty(pos, state, state);
        if (!level.isClientSide) {
            ItemPipeNetwork.invalidate(level);
        }
    }

    private static boolean isStripeBlock(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ItemPipeBlock
                || state.getBlock() instanceof FluidPipeBlock
                || state.getBlock() instanceof GasPipeBlock
                || state.getBlock() instanceof EnergyCableBlock;
    }

    private static void refreshConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ItemPipeBlock pipe) {
            BlockState updated = pipe.updateConnections(level, pos, state);
            level.setBlock(pos, updated, 3);
            return;
        }
        if (state.getBlock() instanceof FluidPipeBlock pipe) {
            BlockState updated = pipe.updateConnections(level, pos, state);
            level.setBlock(pos, updated, 3);
            return;
        }
        if (state.getBlock() instanceof GasPipeBlock pipe) {
            BlockState updated = pipe.updateConnections(level, pos, state);
            level.setBlock(pos, updated, 3);
            return;
        }
        if (state.getBlock() instanceof EnergyCableBlock cable) {
            BlockState updated = cable.updateConnections(level, pos, state);
            level.setBlock(pos, updated, 3);
        }
    }
}
