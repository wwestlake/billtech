package com.billtech.menu;

import com.billtech.block.entity.DistillerBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class DistillerMenu extends MachineMenuBase {
    private final Container ghostContainer;

    public DistillerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(4), null);
    }

    public DistillerMenu(int id, Inventory inventory, DistillerBlockEntity be) {
        this(id, inventory, new SimpleContainer(4), be);
    }

    private DistillerMenu(
            int id,
            Inventory inventory,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.DISTILLER, id, access, EnumSet.of(TransportType.FLUID, TransportType.ENERGY));
        this.ghostContainer = ghostContainer;
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new GhostSlot(ghostContainer, 0, 44, 35));  // input
        addSlot(new GhostSlot(ghostContainer, 1, 98, 35));  // light
        addSlot(new GhostSlot(ghostContainer, 2, 116, 35)); // heavy
        addSlot(new GhostSlot(ghostContainer, 3, 134, 35)); // sludge
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
