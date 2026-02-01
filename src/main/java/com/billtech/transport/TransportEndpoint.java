package com.billtech.transport;

import net.minecraft.core.BlockPos;

public interface TransportEndpoint<T> {
    BlockPos pos();

    boolean canProvide(T payload);

    boolean canAccept(T payload);

    long simulateExtract(T payload, long maxAmount);

    long extract(T payload, long amount);

    long simulateInsert(T payload, long maxAmount);

    long insert(T payload, long amount);
}
