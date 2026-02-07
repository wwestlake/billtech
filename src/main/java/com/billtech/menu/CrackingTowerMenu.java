package com.billtech.menu;

import com.billtech.block.entity.CrackingTowerControllerBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class CrackingTowerMenu extends MachineMenuBase {
    private final Container ghostContainer;

    public CrackingTowerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(5), null);
    }

    public CrackingTowerMenu(int id, Inventory inventory, CrackingTowerControllerBlockEntity be) {
        this(id, inventory, new SimpleContainer(5), be);
    }

    private CrackingTowerMenu(
            int id,
            Inventory inventory,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.CRACKING_TOWER, id, access, EnumSet.of(TransportType.FLUID, TransportType.ENERGY));
        this.ghostContainer = ghostContainer;
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new GhostSlot(ghostContainer, 0, 44, 35));  // input
        addSlot(new GhostSlot(ghostContainer, 1, 98, 26));  // light
        addSlot(new GhostSlot(ghostContainer, 2, 116, 26)); // medium
        addSlot(new GhostSlot(ghostContainer, 3, 98, 44));  // heavy
        addSlot(new GhostSlot(ghostContainer, 4, 116, 44)); // residue
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
