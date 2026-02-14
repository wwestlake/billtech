package com.billtech.menu;

import com.billtech.block.entity.StripeBenchBlockEntity;
import com.billtech.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public class StripeBenchMenu extends AbstractContainerMenu {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_PLASTIC = 1;
    private static final int SLOT_DYE1 = 2;
    private static final int SLOT_DYE2 = 3;
    private static final int SLOT_DYE3 = 4;
    private static final int SLOT_OUTPUT = 5;
    private static final int MACHINE_SLOTS = 6;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container machineContainer;

    public StripeBenchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS));
    }

    public StripeBenchMenu(int id, Inventory inventory, StripeBenchBlockEntity be) {
        this(id, inventory, (Container) be);
    }

    private StripeBenchMenu(int id, Inventory inventory, Container machineContainer) {
        super(ModMenus.STRIPE_BENCH, id);
        this.machineContainer = machineContainer;
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new PipeSlot(machineContainer, SLOT_INPUT, 26, 35));
        addSlot(new PlasticSlot(machineContainer, SLOT_PLASTIC, 44, 35));
        addSlot(new DyeSlot(machineContainer, SLOT_DYE1, 80, 17));
        addSlot(new DyeSlot(machineContainer, SLOT_DYE2, 80, 35));
        addSlot(new DyeSlot(machineContainer, SLOT_DYE3, 80, 53));
        addSlot(new OutputSlot(machineContainer, SLOT_OUTPUT, 134, 35));
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
    public boolean stillValid(Player player) {
        return machineContainer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();

        if (index == SLOT_OUTPUT) {
            if (!moveItemStackTo(source, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(source, copy);
        } else if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, PLAYER_INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (source.is(ModItems.PLASTIC_SHEET)) {
                if (!moveItemStackTo(source, SLOT_PLASTIC, SLOT_PLASTIC + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (source.getItem() instanceof DyeItem) {
                if (!moveItemStackTo(source, SLOT_DYE1, SLOT_DYE3 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slots.get(SLOT_INPUT).mayPlace(source)) {
                if (!moveItemStackTo(source, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (source.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, source);
        return copy;
    }

    private static final class PipeSlot extends Slot {
        private PipeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StripeBenchBlockEntity.isStripeCapable(stack);
        }
    }

    private static final class PlasticSlot extends Slot {
        private PlasticSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.PLASTIC_SHEET);
        }
    }

    private static final class DyeSlot extends Slot {
        private DyeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof DyeItem;
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
