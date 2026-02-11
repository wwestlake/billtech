package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.crafting.RecipeCardData;
import com.billtech.menu.RecipeEncoderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeEncoderBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SLOT_PATTERN_START = 0;
    public static final int SLOT_PATTERN_COUNT = 9;
    public static final int SLOT_PLASTIC = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);

    public RecipeEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECIPE_ENCODER, pos, state);
    }

    public Optional<RecipeHolder<CraftingRecipe>> findRecipe(Level level) {
        if (level == null || level.isClientSide) {
            return Optional.empty();
        }
        var server = level.getServer();
        if (server == null) {
            return Optional.empty();
        }
        CraftingInput input = buildInput();
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    public CraftingInput buildInput() {
        List<ItemStack> pattern = new ArrayList<>(RecipeCardData.GRID_SIZE);
        for (int i = 0; i < RecipeCardData.GRID_SIZE; i++) {
            ItemStack stack = items.get(i);
            pattern.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        return CraftingInput.of(3, 3, pattern);
    }

    public List<ItemStack> getPatternStacks() {
        List<ItemStack> pattern = new ArrayList<>(RecipeCardData.GRID_SIZE);
        for (int i = 0; i < RecipeCardData.GRID_SIZE; i++) {
            pattern.add(items.get(i));
        }
        return pattern;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.recipe_encoder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RecipeEncoderMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
