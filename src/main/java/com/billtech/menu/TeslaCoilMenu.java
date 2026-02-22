package com.billtech.menu;

import com.billtech.block.entity.TeslaCoilBlockEntity;
import com.billtech.transport.TransportType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class TeslaCoilMenu extends MachineMenuBase {
    public TeslaCoilMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public TeslaCoilMenu(int id, Inventory inventory, TeslaCoilBlockEntity be) {
        super(ModMenus.TESLA_COIL, id, be, EnumSet.of(TransportType.ENERGY));
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
