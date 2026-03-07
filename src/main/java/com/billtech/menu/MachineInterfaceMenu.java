package com.billtech.menu;

import com.billtech.automation.MachineCardData;
import com.billtech.block.entity.MachineInterfaceBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MachineInterfaceMenu extends AbstractContainerMenu {
    public static final int ACTION_ENABLE = 0;
    public static final int ACTION_DISABLE = 1;

    private final Container container;
    private final MachineInterfaceBlockEntity machineInterface;
    private final DataSlot hasTarget;
    private final DataSlot remoteEnabled;
    private final DataSlot targetBlockId;

    public MachineInterfaceMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), null);
    }

    public MachineInterfaceMenu(int id, Inventory inventory, MachineInterfaceBlockEntity machineInterface) {
        this(id, inventory, machineInterface, machineInterface);
    }

    private MachineInterfaceMenu(int id, Inventory inventory, Container container, MachineInterfaceBlockEntity machineInterface) {
        super(ModMenus.MACHINE_INTERFACE, id);
        this.container = container;
        this.machineInterface = machineInterface;
        addSlot(new CardSlot(container, 0, 80, 20));
        addPlayerSlots(inventory, 8, 58);

        this.hasTarget = addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return machineInterface != null && machineInterface.isTargetControllable() ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        });
        this.remoteEnabled = addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return machineInterface != null && machineInterface.isTargetRemoteEnabled() ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        });
        this.targetBlockId = addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return machineInterface == null ? 0 : machineInterface.getTargetBlockId();
            }

            @Override
            public void set(int value) {
            }
        });
    }

    private void addPlayerSlots(Inventory inventory, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, startX + col * 18, hotbarY));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machineInterface == null) {
            return false;
        }
        if (id == ACTION_ENABLE) {
            return machineInterface.setTargetRemoteEnabled(true);
        }
        if (id == ACTION_DISABLE) {
            return machineInterface.setTargetRemoteEnabled(false);
        }
        return false;
    }

    public boolean hasTargetMachine() {
        return hasTarget.get() == 1;
    }

    public boolean isRemoteEnabled() {
        return remoteEnabled.get() == 1;
    }

    public int getTargetBlockId() {
        return targetBlockId.get();
    }

    public String getCardLabel() {
        return machineInterface == null ? "" : machineInterface.getCardLabel();
    }

    @Override
    public boolean stillValid(Player player) {
        return machineInterface == null || machineInterface.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class CardSlot extends Slot {
        private CardSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MachineCardData.isMachineCard(stack);
        }
    }
}
