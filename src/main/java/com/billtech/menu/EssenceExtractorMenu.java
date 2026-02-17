package com.billtech.menu;

import com.billtech.block.entity.EssenceExtractorBlockEntity;
import com.billtech.transport.TransportType;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class EssenceExtractorMenu extends MachineMenuBase {
    private final Container upgradeContainer;

    public EssenceExtractorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(3), null);
    }

    public EssenceExtractorMenu(int id, Inventory inventory, EssenceExtractorBlockEntity be) {
        this(id, inventory, be.getUpgradeContainer(), be);
    }

    private EssenceExtractorMenu(int id, Inventory inventory, Container upgradeContainer, EssenceExtractorBlockEntity be) {
        super(ModMenus.ESSENCE_EXTRACTOR, id, be, EnumSet.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY));
        this.upgradeContainer = upgradeContainer;
        addUpgradeSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addUpgradeSlots() {
        addSlot(new TypedUpgradeSlot(upgradeContainer, 0, 8, 22, UpgradeType.SPEED, 4));
        addSlot(new TypedUpgradeSlot(upgradeContainer, 1, 26, 22, UpgradeType.MULTI_KILL, 2));
        addSlot(new TypedUpgradeSlot(upgradeContainer, 2, 44, 22, UpgradeType.YIELD, 3));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class TypedUpgradeSlot extends Slot {
        private final UpgradeType acceptedType;
        private final int maxStackSize;

        private TypedUpgradeSlot(Container container, int slot, int x, int y, UpgradeType acceptedType, int maxStackSize) {
            super(container, slot, x, y);
            this.acceptedType = acceptedType;
            this.maxStackSize = maxStackSize;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == acceptedType;
        }

        @Override
        public int getMaxStackSize() {
            return maxStackSize;
        }
    }
}
