package com.billtech.item;

import com.billtech.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BillTechBucketItem extends BucketItem {
    public BillTechBucketItem(Properties properties) {
        super(net.minecraft.world.level.material.Fluids.EMPTY, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            FluidState fluidState = level.getFluidState(pos);
            if (player.isCreative() && fluidState.getType() == ModFluids.SLUDGE && fluidState.isSource()) {
                if (!level.isClientSide) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
        }
        return super.use(level, player, hand);
    }
}
