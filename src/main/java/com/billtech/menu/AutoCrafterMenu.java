package com.billtech.menu;

import com.billtech.block.entity.AutoCrafterBlockEntity;
import com.billtech.crafting.RecipeCardData;
import com.billtech.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AutoCrafterMenu extends AbstractContainerMenu {
    private final Container container;
    private final AutoCrafterBlockEntity crafter;

    public AutoCrafterMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), null);
    }

    public AutoCrafterMenu(int id, Inventory inventory, AutoCrafterBlockEntity crafter) {
        this(id, inventory, crafter, crafter);
    }

    private AutoCrafterMenu(int id, Inventory inventory, Container container, AutoCrafterBlockEntity crafter) {
        super(ModMenus.AUTO_CRAFTER, id);
        this.container = container;
        this.crafter = crafter;
        addCardSlot();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addCardSlot() {
        addSlot(new CardSlot(container, 0, 80, 35));
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

    public ItemStack getCardStack() {
        return container.getItem(0);
    }

    public java.util.List<ItemStack> getPattern() {
        return RecipeCardData.getPattern(getCardStack());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crafter == null || player.level().isClientSide) {
            return false;
        }
        int requested = switch (id) {
            case 0 -> 1;
            case 1 -> 16;
            case 2 -> Integer.MAX_VALUE;
            default -> 0;
        };
        if (requested <= 0) {
            return false;
        }
        crafter.craftByItems(player.level(), requested);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return crafter == null || crafter.stillValid(player);
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
            return stack.getItem() == ModItems.RECIPE_CARD;
        }
    }
}
