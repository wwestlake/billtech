package com.billtech.menu;

import com.billtech.block.entity.CoalPyrolyzerBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class CoalPyrolyzerMenu extends MachineMenuBase {
    private static final int SLOT_INPUT = 0;
    private final Container machineContainer;
    private final Container ghostContainer;

    public CoalPyrolyzerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainer(1), null);
    }

    public CoalPyrolyzerMenu(int id, Inventory inventory, CoalPyrolyzerBlockEntity be) {
        this(id, inventory, be, new SimpleContainer(1), be);
    }

    private CoalPyrolyzerMenu(
            int id,
            Inventory inventory,
            Container machineContainer,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.COAL_PYROLYZER, id, access, EnumSet.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY));
        this.machineContainer = machineContainer;
        this.ghostContainer = ghostContainer;
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new Slot(machineContainer, SLOT_INPUT, 56, 35));
        addSlot(new GhostSlot(ghostContainer, 0, 116, 35));
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
