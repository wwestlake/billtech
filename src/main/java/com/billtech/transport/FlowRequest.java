package com.billtech.transport;

import net.minecraft.core.BlockPos;

public record FlowRequest<T>(T payload, long amount, BlockPos fromPos, long time) {
}
