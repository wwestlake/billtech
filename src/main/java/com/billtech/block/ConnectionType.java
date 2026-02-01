package com.billtech.block;

import net.minecraft.util.StringRepresentable;

public enum ConnectionType implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    ENDPOINT("endpoint");

    private final String name;

    ConnectionType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
