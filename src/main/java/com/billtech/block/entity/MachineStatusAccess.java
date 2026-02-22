package com.billtech.block.entity;

public interface MachineStatusAccess {
    default int clampLong(long value) {
        if (value <= 0) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    int getEnergyStored();

    int getEnergyCapacity();

    int getFluidInStored();

    int getFluidInCapacity();

    int getFluidOutStored();

    int getFluidOutCapacity();

    default MachineRuntimeState getRuntimeState() {
        return MachineRuntimeState.IDLE;
    }

    default int getRuntimeStateId() {
        return getRuntimeState().id();
    }

    default int getProcessProgress() {
        return 0;
    }

    default int getProcessMax() {
        return 0;
    }

    default int getInputBufferUsed() {
        return getFluidInStored();
    }

    default int getInputBufferCapacity() {
        return getFluidInCapacity();
    }

    default int getOutputBufferUsed() {
        return getFluidOutStored();
    }

    default int getOutputBufferCapacity() {
        return getFluidOutCapacity();
    }

    default boolean supportsRemoteControl() {
        return this instanceof RemoteControllable;
    }

    default boolean isRemoteEnabled() {
        if (this instanceof RemoteControllable controllable) {
            return controllable.isRemoteEnabled();
        }
        return true;
    }
}
