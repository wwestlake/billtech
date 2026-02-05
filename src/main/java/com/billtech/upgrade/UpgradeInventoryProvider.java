package com.billtech.upgrade;

import net.minecraft.world.Container;

public interface UpgradeInventoryProvider {
    Container getUpgradeContainer();

    int getUpgradeCount(UpgradeType type);
}
