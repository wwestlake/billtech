package com.billtech.transport;

import net.minecraft.core.BlockPos;

public record PipeNode<T>(BlockPos pos, PipeTier<T> tier) {
}
