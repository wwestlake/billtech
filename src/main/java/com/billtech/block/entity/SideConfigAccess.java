package com.billtech.block.entity;

import net.minecraft.core.Direction;

public interface SideConfigAccess {
    SideConfig getSideConfig();

    Direction getFacing();
}
