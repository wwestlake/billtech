package com.billtech.menu;

import com.billtech.block.entity.PortMode;
import com.billtech.block.entity.SideConfig;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;

public final class SideConfigData implements ContainerData {
    private static final int SIDE_COUNT = TransportType.values().length * 6;
    private static final int FACING_INDEX = SIDE_COUNT;
    private final SideConfigAccess access;
    private final int[] cached = new int[FACING_INDEX + 1];

    public SideConfigData(SideConfigAccess access) {
        this.access = access;
    }

    @Override
    public int get(int index) {
        if (access == null) {
            return cached[index];
        }
        if (index == FACING_INDEX) {
            return access.getFacing().get3DDataValue();
        }
        TransportType type = TransportType.values()[index / 6];
        Direction dir = Direction.from3DDataValue(index % 6);
        SideConfig config = access.getSideConfig();
        return config.get(type, dir).ordinal();
    }

    @Override
    public void set(int index, int value) {
        cached[index] = value;
    }

    @Override
    public int getCount() {
        return FACING_INDEX + 1;
    }

    public int getSideCount() {
        return SIDE_COUNT;
    }

    public int getIndex(TransportType type, Direction dir) {
        return type.ordinal() * 6 + dir.get3DDataValue();
    }

    public PortMode getMode(TransportType type, Direction dir) {
        int raw = get(getIndex(type, dir));
        PortMode[] values = PortMode.values();
        if (raw < 0 || raw >= values.length) {
            return PortMode.NONE;
        }
        return values[raw];
    }

    public Direction getFacing() {
        return Direction.from3DDataValue(get(FACING_INDEX));
    }
}
