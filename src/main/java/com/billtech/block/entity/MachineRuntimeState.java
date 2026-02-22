package com.billtech.block.entity;

public enum MachineRuntimeState {
    IDLE(0),
    RUNNING(1),
    DISABLED(2),
    NO_POWER(3),
    OUTPUT_FULL(4),
    NO_WORK(5),
    BLOCKED(6),
    ERROR(7);

    private final int id;

    MachineRuntimeState(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static MachineRuntimeState fromId(int id) {
        for (MachineRuntimeState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }
}

