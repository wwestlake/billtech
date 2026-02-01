package com.billtech.transport;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Optional;

public interface TransportNetwork<T> {
    TransportType transportType();

    Optional<PipeNode<T>> pipeAt(BlockPos pos);

    Collection<PipeNode<T>> pipes();

    Collection<TransportEndpoint<T>> endpoints();

    Collection<TransportEndpoint<T>> endpointsAt(BlockPos pos);
}
