package com.billtech.transport;

import java.util.Objects;
import java.util.function.Predicate;

public record SimplePipeTier<T>(
        String id,
        TransportType transportType,
        long maxRate,
        int maxDistance,
        Predicate<T> allowed
) implements PipeTier<T> {

    public SimplePipeTier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transportType, "transportType");
    }

    @Override
    public boolean allows(T payload) {
        return allowed == null || allowed.test(payload);
    }
}
