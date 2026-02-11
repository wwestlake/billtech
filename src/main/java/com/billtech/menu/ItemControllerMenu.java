package com.billtech.menu;

import com.billtech.block.entity.ItemControllerBlockEntity;
import com.billtech.pipe.ItemPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemControllerMenu extends AbstractContainerMenu {
    private static final int DISPLAY_COLUMNS = 9;
    private static final int DISPLAY_ROWS = 5;
    private static final int DISPLAY_SIZE = DISPLAY_COLUMNS * DISPLAY_ROWS;
    private static final int MAX_COUNT_DISPLAY = 999;

    private final Container displayContainer = new SimpleContainer(DISPLAY_SIZE);
    private final ItemControllerBlockEntity controller;
    private final DataSlot pageSlot = DataSlot.standalone();
    private final DataSlot pageCountSlot = DataSlot.standalone();
    private final List<ItemPipeNetwork.ItemEntry> snapshot = new ArrayList<>();
    private final List<ItemPipeNetwork.ItemEntry> filtered = new ArrayList<>();
    private final ItemVariant[] displayVariants = new ItemVariant[DISPLAY_SIZE];
    private String searchText = "";

    public ItemControllerMenu(int id, Inventory inventory, ItemControllerBlockEntity controller) {
        super(ModMenus.ITEM_CONTROLLER, id);
        this.controller = controller;
        addDisplaySlots();
        addPlayerSlots(inventory, 8, 140);
        addDataSlot(pageSlot);
        addDataSlot(pageCountSlot);
        refreshSnapshot();
    }

    public ItemControllerMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    private void addDisplaySlots() {
        int index = 0;
        int startX = 8;
        int startY = 18;
        for (int row = 0; row < DISPLAY_ROWS; row++) {
            for (int col = 0; col < DISPLAY_COLUMNS; col++) {
                addSlot(new DisplaySlot(displayContainer, index++, startX + col * 18, startY + row * 18));
            }
        }
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

    public void setSearchText(String searchText) {
        this.searchText = searchText == null ? "" : searchText.trim().toLowerCase(java.util.Locale.ROOT);
        refreshFilter();
        fillDisplay();
    }

    public void nextPage() {
        int page = pageSlot.get();
        int pages = Math.max(1, pageCountSlot.get());
        if (page + 1 < pages) {
            pageSlot.set(page + 1);
            fillDisplay();
        }
    }

    public void prevPage() {
        int page = pageSlot.get();
        if (page > 0) {
            pageSlot.set(page - 1);
            fillDisplay();
        }
    }

    public int getPage() {
        return pageSlot.get();
    }

    public int getPageCount() {
        return Math.max(1, pageCountSlot.get());
    }

    public ItemStack getDisplayStack(int slot) {
        if (slot < 0 || slot >= DISPLAY_SIZE) {
            return ItemStack.EMPTY;
        }
        return displayContainer.getItem(slot);
    }

    public void requestCraft(ItemStack stack, int amount) {
        if (controller == null || controller.getLevel() == null || amount <= 0 || stack.isEmpty()) {
            return;
        }
        controller.requestCraft(controller.getLevel(), ItemVariant.of(stack), amount);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        refreshSnapshot();
    }

    private void refreshSnapshot() {
        if (controller == null || controller.getLevel() == null) {
            return;
        }
        snapshot.clear();
        snapshot.addAll(controller.getSnapshot(controller.getLevel()));
        refreshFilter();
        fillDisplay();
    }

    private void refreshFilter() {
        filtered.clear();
        if (searchText.isEmpty()) {
            filtered.addAll(snapshot);
        } else {
            for (ItemPipeNetwork.ItemEntry entry : snapshot) {
                ItemStack stack = entry.variant().toStack();
                String name = stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
                if (name.contains(searchText)) {
                    filtered.add(entry);
                }
            }
        }
        int pages = Math.max(1, (int) Math.ceil(filtered.size() / (double) DISPLAY_SIZE));
        pageCountSlot.set(pages);
        if (pageSlot.get() >= pages) {
            pageSlot.set(pages - 1);
        }
    }

    private void fillDisplay() {
        int page = pageSlot.get();
        int start = page * DISPLAY_SIZE;
        for (int i = 0; i < DISPLAY_SIZE; i++) {
            int index = start + i;
            if (index < filtered.size()) {
                ItemPipeNetwork.ItemEntry entry = filtered.get(index);
                long amount = entry.amount();
                ItemStack stack = entry.variant().toStack();
                int count = (int) Math.min(amount, MAX_COUNT_DISPLAY);
                stack.setCount(Math.max(1, count));
                displayContainer.setItem(i, stack);
                displayVariants[i] = entry.variant();
            } else {
                displayContainer.setItem(i, ItemStack.EMPTY);
                displayVariants[i] = null;
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return controller == null || controller.getLevel() == null || player.distanceToSqr(
                controller.getBlockPos().getX() + 0.5,
                controller.getBlockPos().getY() + 0.5,
                controller.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            prevPage();
            return true;
        }
        if (id == 1) {
            nextPage();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < DISPLAY_SIZE && controller != null && controller.getLevel() != null) {
            ItemVariant variant = displayVariants[slotId];
            if (variant != null && !variant.isBlank()) {
                long amount = button == 1 ? 1 : variant.getItem().getDefaultMaxStackSize();
                long extracted;
                try (Transaction tx = Transaction.openOuter()) {
                    extracted = ItemPipeNetwork.extractFromNetwork(
                            controller.getLevel(),
                            controller.getBlockPos(),
                            variant,
                            amount,
                            tx,
                            null
                    );
                    if (extracted > 0) {
                        tx.commit();
                    }
                }
                if (extracted > 0) {
                    ItemStack stack = variant.toStack((int) extracted);
                    player.getInventory().placeItemBackInInventory(stack);
                }
                refreshSnapshot();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class DisplaySlot extends Slot {
        private DisplaySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }
    }
}
