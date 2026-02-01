package com.billtech.transport;

public interface FlowExecutor<T> {
    long execute(TransportNetwork<T> network, PipePath path, FlowRequest<T> request);
}
