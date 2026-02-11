package com.billtech.menu;

import com.billtech.block.entity.RecipeEncoderBlockEntity;
import com.billtech.crafting.RecipeCardData;
import com.billtech.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

public class RecipeEncoderMenu extends AbstractContainerMenu {
    private final Container container;
    private final RecipeEncoderBlockEntity encoder;

    public RecipeEncoderMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(10), null);
    }

    public RecipeEncoderMenu(int id, Inventory inventory, RecipeEncoderBlockEntity encoder) {
        this(id, inventory, encoder, encoder);
    }

    private RecipeEncoderMenu(int id, Inventory inventory, Container container, RecipeEncoderBlockEntity encoder) {
        super(ModMenus.RECIPE_ENCODER, id);
        this.container = container;
        this.encoder = encoder;
        addRecipeSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addRecipeSlots() {
        int startX = 30;
        int startY = 18;
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(container, index++, startX + col * 18, startY + row * 18));
            }
        }
        addSlot(new PlasticSlot(container, RecipeEncoderBlockEntity.SLOT_PLASTIC, 124, 36));
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
        if (id != 0 || encoder == null || player.level().isClientSide) {
            return false;
        }
        ItemStack plastic = container.getItem(RecipeEncoderBlockEntity.SLOT_PLASTIC);
        if (plastic.isEmpty() || plastic.getItem() != ModItems.PLASTIC_SHEET) {
            return false;
        }
        Optional<RecipeHolder<CraftingRecipe>> recipe = encoder.findRecipe(player.level());
        if (recipe.isEmpty()) {
            return false;
        }
        var holder = recipe.get();
        ItemStack output = holder.value().assemble(encoder.buildInput(), player.level().registryAccess());
        if (output.isEmpty()) {
            return false;
        }
        ItemStack card = RecipeCardData.createCard(holder.id(), encoder.getPatternStacks(), output);
        container.removeItem(RecipeEncoderBlockEntity.SLOT_PLASTIC, 1);
        player.getInventory().placeItemBackInInventory(card);
        for (int i = 0; i < RecipeEncoderBlockEntity.SLOT_PATTERN_COUNT; i++) {
            ItemStack stack = container.removeItem(i, container.getItem(i).getCount());
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        encoder.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return encoder == null || encoder.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class PlasticSlot extends Slot {
        private PlasticSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == ModItems.PLASTIC_SHEET;
        }
    }
}
