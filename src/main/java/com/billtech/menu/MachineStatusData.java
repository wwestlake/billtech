package com.billtech.menu;

import com.billtech.block.entity.MachineStatusAccess;
import net.minecraft.world.inventory.ContainerData;

public final class MachineStatusData implements ContainerData {
    public static final int ENERGY_STORED = 0;
    public static final int ENERGY_CAPACITY = 1;
    public static final int FLUID_IN_STORED = 2;
    public static final int FLUID_IN_CAPACITY = 3;
    public static final int FLUID_OUT_STORED = 4;
    public static final int FLUID_OUT_CAPACITY = 5;
    public static final int PROCESS_PROGRESS = 6;
    public static final int PROCESS_MAX = 7;
    public static final int RUNTIME_STATE = 8;
    public static final int REMOTE_ENABLED = 9;
    public static final int REMOTE_SUPPORTED = 10;

    private static final int COUNT = 11;
    private final MachineStatusAccess access;
    private final int[] cached = new int[COUNT];

    public MachineStatusData(MachineStatusAccess access) {
        this.access = access;
    }

    @Override
    public int get(int index) {
        if (access == null) {
            return cached[index];
        }
        return switch (index) {
            case ENERGY_STORED -> access.getEnergyStored();
            case ENERGY_CAPACITY -> access.getEnergyCapacity();
            case FLUID_IN_STORED -> access.getFluidInStored();
            case FLUID_IN_CAPACITY -> access.getFluidInCapacity();
            case FLUID_OUT_STORED -> access.getFluidOutStored();
            case FLUID_OUT_CAPACITY -> access.getFluidOutCapacity();
            case PROCESS_PROGRESS -> access.getProcessProgress();
            case PROCESS_MAX -> access.getProcessMax();
            case RUNTIME_STATE -> access.getRuntimeStateId();
            case REMOTE_ENABLED -> access.isRemoteEnabled() ? 1 : 0;
            case REMOTE_SUPPORTED -> access.supportsRemoteControl() ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        cached[index] = value;
    }

    @Override
    public int getCount() {
        return COUNT;
    }
}
