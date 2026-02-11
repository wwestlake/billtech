package com.billtech.crafting;

import com.billtech.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

public final class RecipeCardData {
    public static final int GRID_SIZE = 9;

    private static final String TAG_RECIPE = "Recipe";
    private static final String TAG_PATTERN = "Pattern";
    private static final String TAG_OUTPUT = "Output";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_COUNT = "Count";

    private RecipeCardData() {
    }

    public static ItemStack createCard(
            ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeId,
            List<ItemStack> pattern,
            ItemStack output
    ) {
        ItemStack card = new ItemStack(ModItems.RECIPE_CARD);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_RECIPE, recipeId.location().toString());
        tag.put(TAG_PATTERN, encodePattern(pattern));
        tag.put(TAG_OUTPUT, encodeStack(output));
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return card;
    }

    public static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> getRecipeKey(ItemStack card) {
        CompoundTag tag = getData(card);
        if (tag == null || !tag.contains(TAG_RECIPE)) {
            return null;
        }
        String idString = tag.getString(TAG_RECIPE).orElse("");
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null) {
            return null;
        }
        return ResourceKey.create(Registries.RECIPE, id);
    }

    public static List<ItemStack> getPattern(ItemStack card) {
        CompoundTag tag = getData(card);
        List<ItemStack> pattern = new java.util.ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) {
            pattern.add(ItemStack.EMPTY);
        }
        if (tag == null || !tag.contains(TAG_PATTERN)) {
            return pattern;
        }
        var tagValue = tag.get(TAG_PATTERN);
        if (tagValue instanceof ListTag list) {
            int limit = Math.min(list.size(), GRID_SIZE);
            for (int i = 0; i < limit; i++) {
                CompoundTag itemTag = list.getCompound(i).orElse(new CompoundTag());
                pattern.set(i, decodeStack(itemTag));
            }
        }
        return pattern;
    }

    public static ItemStack getOutput(ItemStack card) {
        CompoundTag tag = getData(card);
        if (tag == null || !tag.contains(TAG_OUTPUT)) {
            return ItemStack.EMPTY;
        }
        return decodeStack(tag.getCompound(TAG_OUTPUT).orElse(new CompoundTag()));
    }

    private static ListTag encodePattern(List<ItemStack> pattern) {
        ListTag list = new ListTag();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = i < pattern.size() ? pattern.get(i) : ItemStack.EMPTY;
            list.add(encodeStack(stack));
        }
        return list;
    }

    private static CompoundTag encodeStack(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        if (stack.isEmpty()) {
            return tag;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return tag;
        }
        tag.putString(TAG_ITEM, id.toString());
        tag.putInt(TAG_COUNT, Math.max(1, stack.getCount()));
        return tag;
    }

    private static ItemStack decodeStack(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_ITEM)) {
            return ItemStack.EMPTY;
        }
        String idString = tag.getString(TAG_ITEM).orElse("");
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Optional<Item> item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty() || item.get() == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        int count = Math.max(1, tag.getInt(TAG_COUNT).orElse(0));
        return new ItemStack(item.get(), count);
    }

    private static CompoundTag getData(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != ModItems.RECIPE_CARD) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }
}
