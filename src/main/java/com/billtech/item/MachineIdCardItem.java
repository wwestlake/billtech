package com.billtech.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class MachineIdCardItem extends Item {
    public MachineIdCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Card identity is managed when installed into interface/control devices.
        return InteractionResult.PASS;
    }
}
