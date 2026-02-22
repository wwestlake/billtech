package com.billtech.block.entity;

public interface RemoteControllable {
    boolean isRemoteEnabled();

    void setRemoteEnabled(boolean enabled);

    default void toggleRemoteEnabled() {
        setRemoteEnabled(!isRemoteEnabled());
    }
}

