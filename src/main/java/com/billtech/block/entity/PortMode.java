package com.billtech.block.entity;

public enum PortMode {
    NONE,
    INPUT,
    OUTPUT,
    BOTH;

    public boolean allowsInsert() {
        return this == INPUT || this == BOTH;
    }

    public boolean allowsExtract() {
        return this == OUTPUT || this == BOTH;
    }

    public PortMode next() {
        return switch (this) {
            case NONE -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> BOTH;
            case BOTH -> NONE;
        };
    }
}
