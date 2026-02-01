package com.billtech.transport;

import net.minecraft.core.BlockPos;

import java.util.List;

public record PipePath(List<BlockPos> nodes, int length, long maxRate) {
}
