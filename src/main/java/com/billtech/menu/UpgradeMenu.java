package com.billtech.menu;

import com.billtech.upgrade.UpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeMenu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOTS = 4;
    private final Container upgradeContainer;

    public UpgradeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(UPGRADE_SLOTS));
    }

    public UpgradeMenu(int containerId, Inventory inventory, Container upgradeContainer) {
        super(ModMenus.UPGRADES, containerId);
        this.upgradeContainer = upgradeContainer;
        addUpgradeSlots();
        addPlayerSlots(inventory);
    }

    private void addUpgradeSlots() {
        int startX = 62;
        int startY = 20;
        int slot = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int x = startX + col * 18;
                int y = startY + row * 18;
                addSlot(new UpgradeSlot(upgradeContainer, slot++, x, y));
            }
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        int startX = 8;
        int startY = 51;
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }
    }
}
