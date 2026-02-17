package com.billtech.menu;

import com.billtech.block.entity.ControlConductorPadBlockEntity;
import com.billtech.transport.TransportType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class ControlConductorPadMenu extends MachineMenuBase {
    public ControlConductorPadMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ControlConductorPadMenu(int id, Inventory inventory, ControlConductorPadBlockEntity be) {
        super(ModMenus.CONTROL_CONDUCTOR_PAD, id, be, EnumSet.noneOf(TransportType.class));
        addPlayerSlots(inventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
