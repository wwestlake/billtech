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
}
