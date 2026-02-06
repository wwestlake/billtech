package com.billtech.cover;

import com.billtech.block.ModBlocks;
import com.billtech.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CoverInteraction {
    private CoverInteraction() {
    }

    public static InteractionResult handle(
            Level level,
            Player player,
            ItemStack stack,
            Direction side,
            CoverProvider coverable
    ) {
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (!coverable.hasCover(side)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                coverable.clearCover(side);
                if (!player.isCreative()) {
                    ItemStack drop = new ItemStack(ModItems.PIPE_COVER);
                    if (!player.addItem(drop)) {
                        player.drop(drop, false);
                    }
                }
            }
            return sideResult(level);
        }

        if (stack.is(ModItems.PIPE_COVER)) {
            if (coverable.hasCover(side)) {
                return sideResult(level);
            }
            if (!level.isClientSide) {
                coverable.setCover(side, defaultCoverId());
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return sideResult(level);
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            if (!coverable.hasCover(side)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                coverable.setCover(side, id);
            }
            return sideResult(level);
        }

        return InteractionResult.PASS;
    }

    private static ResourceLocation defaultCoverId() {
        return BuiltInRegistries.BLOCK.getKey(ModBlocks.COVER_PANEL);
    }

    private static InteractionResult sideResult(Level level) {
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
