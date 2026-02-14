package com.billtech.stripe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class StripeItemData {
    private static final String TAG_ROOT = "BillTechStripe";

    private StripeItemData() {
    }

    public static StripeData read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StripeData.EMPTY;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return StripeData.EMPTY;
        }
        CompoundTag root = customData.copyTag();
        if (!root.contains(TAG_ROOT)) {
            return StripeData.EMPTY;
        }
        CompoundTag stripeTag = root.getCompound(TAG_ROOT).orElse(null);
        return StripeData.load(stripeTag);
    }

    public static ItemStack write(ItemStack stack, StripeData data) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        StripeData safe = data == null ? StripeData.EMPTY : data;
        CompoundTag root = getOrCreateRootTag(stack);
        if (safe.isEmpty()) {
            root.remove(TAG_ROOT);
        } else {
            CompoundTag stripeTag = new CompoundTag();
            safe.save(stripeTag);
            root.put(TAG_ROOT, stripeTag);
        }
        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return stack;
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        return existing == null ? new CompoundTag() : existing.copyTag();
    }
}
