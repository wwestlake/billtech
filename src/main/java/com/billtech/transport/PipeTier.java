package com.billtech.transport;

public interface PipeTier<T> {
    String id();

    TransportType transportType();

    long maxRate();

    int maxDistance();

    boolean allows(T payload);
}
