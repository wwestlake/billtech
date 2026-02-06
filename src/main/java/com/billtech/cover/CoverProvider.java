package com.billtech.cover;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public interface CoverProvider {
    boolean hasCover(Direction side);

    BlockState getCoverState(Direction side);

    void setCover(Direction side, ResourceLocation blockId);

    void clearCover(Direction side);
}
