package com.billtech.menu;

import com.billtech.block.entity.ReactorBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class ReactorMenu extends MachineMenuBase {
    private static final int SLOT_INPUT = 0;
    private final Container machineContainer;
    private final Container ghostContainer;

    public ReactorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainer(2), null);
    }

    public ReactorMenu(int id, Inventory inventory, ReactorBlockEntity be) {
        this(id, inventory, be, new SimpleContainer(2), be);
    }

    private ReactorMenu(
            int id,
            Inventory inventory,
            Container machineContainer,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.REACTOR, id, access, EnumSet.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY));
        this.machineContainer = machineContainer;
        this.ghostContainer = ghostContainer;
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new Slot(machineContainer, SLOT_INPUT, 56, 35));
        addSlot(new GhostSlot(ghostContainer, 0, 26, 35)); // water input
        addSlot(new GhostSlot(ghostContainer, 1, 116, 35)); // reaction output
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
