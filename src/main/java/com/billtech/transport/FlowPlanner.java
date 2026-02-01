package com.billtech.transport;

import java.util.Optional;

public interface FlowPlanner<T> {
    Optional<PipePath> plan(TransportNetwork<T> network, FlowRequest<T> request);
}
