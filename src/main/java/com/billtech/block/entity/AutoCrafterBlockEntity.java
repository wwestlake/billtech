package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.crafting.RecipeCardData;
import com.billtech.menu.AutoCrafterMenu;
import com.billtech.pipe.ItemPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AutoCrafterBlockEntity extends BlockEntity implements MenuProvider, Container {
    private static final int SLOT_CARD = 0;
    private static final int CARD_COUNT = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CARD_COUNT, ItemStack.EMPTY);

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER, pos, state);
    }

    public ItemStack getCardStack() {
        return items.get(SLOT_CARD);
    }

    public Optional<RecipeHolder<CraftingRecipe>> getRecipe(Level level) {
        ItemStack card = getCardStack();
        if (card.isEmpty()) {
            return Optional.empty();
        }
        if (level == null || level.isClientSide) {
            return Optional.empty();
        }
        var server = level.getServer();
        if (server == null) {
            return Optional.empty();
        }
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = RecipeCardData.getRecipeKey(card);
        if (key == null) {
            return Optional.empty();
        }
        return server.getRecipeManager().byKey(key)
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> (RecipeHolder<CraftingRecipe>) holder);
    }

    public ItemStack getRecipeOutput(Level level) {
        ItemStack card = getCardStack();
        ItemStack output = RecipeCardData.getOutput(card);
        if (!output.isEmpty()) {
            return output;
        }
        return getRecipe(level)
                .map(recipe -> recipe.value().assemble(buildInput(card), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    public boolean matchesOutput(Level level, ItemVariant target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        ItemStack output = getRecipeOutput(level);
        return !output.isEmpty() && ItemVariant.of(output).equals(target);
    }

    public int craftByItems(Level level, int requestedItems) {
        if (requestedItems <= 0) {
            return 0;
        }
        int crafted = 0;
        int remaining = requestedItems;
        while (remaining > 0) {
            int outputCount = craftOnce(level);
            if (outputCount <= 0) {
                break;
            }
            crafted += outputCount;
            remaining -= outputCount;
        }
        return crafted;
    }

    private int craftOnce(Level level) {
        ItemStack card = getCardStack();
        if (card.isEmpty()) {
            return 0;
        }
        Optional<RecipeHolder<CraftingRecipe>> recipeHolder = getRecipe(level);
        if (recipeHolder.isEmpty()) {
            return 0;
        }
        CraftingInput input = buildInput(card);
        if (input.isEmpty()) {
            return 0;
        }
        CraftingRecipe recipe = recipeHolder.get().value();
        if (!recipe.matches(input, level)) {
            return 0;
        }
        Map<ItemVariant, Integer> required = buildRequirements(input);
        Map<ItemVariant, Long> available = new HashMap<>();
        for (ItemPipeNetwork.ItemEntry entry : ItemPipeNetwork.collectItems(level, worldPosition)) {
            available.merge(entry.variant(), entry.amount(), Long::sum);
        }
        for (Map.Entry<ItemVariant, Integer> entry : required.entrySet()) {
            long have = available.getOrDefault(entry.getKey(), 0L);
            if (have < entry.getValue()) {
                return 0;
            }
        }
        ItemStack output = recipe.assemble(input, level.registryAccess());
        if (output.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.openOuter()) {
            for (Map.Entry<ItemVariant, Integer> entry : required.entrySet()) {
                long extracted = ItemPipeNetwork.extractFromNetwork(
                        level,
                        worldPosition,
                        entry.getKey(),
                        entry.getValue(),
                        tx,
                        null
                );
                if (extracted < entry.getValue()) {
                    return 0;
                }
            }
            long inserted = ItemPipeNetwork.insertIntoNetwork(
                    level,
                    worldPosition,
                    ItemVariant.of(output),
                    output.getCount(),
                    tx,
                    null
            );
            if (inserted < output.getCount()) {
                return 0;
            }
            var remainingItems = recipe.getRemainingItems(input);
            for (ItemStack remainder : remainingItems) {
                if (remainder.isEmpty()) {
                    continue;
                }
                long remInserted = ItemPipeNetwork.insertIntoNetwork(
                        level,
                        worldPosition,
                        ItemVariant.of(remainder),
                        remainder.getCount(),
                        tx,
                        null
                );
                if (remInserted < remainder.getCount()) {
                    return 0;
                }
            }
            tx.commit();
        }
        return output.getCount();
    }

    private static CraftingInput buildInput(ItemStack card) {
        List<ItemStack> pattern = RecipeCardData.getPattern(card);
        for (int i = 0; i < pattern.size(); i++) {
            ItemStack stack = pattern.get(i);
            if (!stack.isEmpty()) {
                pattern.set(i, stack.copyWithCount(1));
            }
        }
        return CraftingInput.of(3, 3, pattern);
    }

    private static Map<ItemVariant, Integer> buildRequirements(CraftingInput input) {
        Map<ItemVariant, Integer> required = new HashMap<>();
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemVariant variant = ItemVariant.of(stack.copyWithCount(1));
            required.merge(variant, 1, Integer::sum);
        }
        return required;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.auto_crafter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoCrafterMenu(id, inventory, this);
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
