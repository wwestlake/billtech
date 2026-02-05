package com.billtech.menu;

import com.billtech.block.entity.BasicCombustionGeneratorBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class BasicCombustionGeneratorMenu extends MachineMenuBase {
    private static final int SLOT_FUEL = 0;
    private final Container machineContainer;

    public BasicCombustionGeneratorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainer(4), null);
    }

    public BasicCombustionGeneratorMenu(int id, Inventory inventory, BasicCombustionGeneratorBlockEntity be) {
        this(id, inventory, be, be.getUpgradeContainer(), be);
    }

    private BasicCombustionGeneratorMenu(
            int id,
            Inventory inventory,
            Container machineContainer,
            Container upgradeContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.BASIC_COMBUSTION_GENERATOR, id, access, EnumSet.of(TransportType.ITEM, TransportType.ENERGY));
        this.machineContainer = machineContainer;
        addMachineSlots();
        addUpgradeSlots(upgradeContainer, 8, 20);
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new Slot(machineContainer, SLOT_FUEL, 56, 35));
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
