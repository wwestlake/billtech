package com.billtech.automation;

import com.billtech.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class MachineCardData {
    private static final String TAG_CARD_ID = "CardId";
    private static final String TAG_LABEL = "Label";

    private MachineCardData() {
    }

    public static boolean isMachineCard(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.MACHINE_ID_CARD;
    }

    public static void ensureIdentity(ItemStack stack, String fallbackLabel) {
        if (!isMachineCard(stack)) {
            return;
        }
        CompoundTag data = getDataOrCreate(stack);
        if (!data.contains(TAG_CARD_ID)) {
            data.putString(TAG_CARD_ID, UUID.randomUUID().toString());
        }
        if (!data.contains(TAG_LABEL) || data.getString(TAG_LABEL).orElse("").isBlank()) {
            String label = (fallbackLabel == null || fallbackLabel.isBlank()) ? "Unlabeled Machine" : fallbackLabel;
            data.putString(TAG_LABEL, label);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static void setLabel(ItemStack stack, String label) {
        if (!isMachineCard(stack)) {
            return;
        }
        CompoundTag data = getDataOrCreate(stack);
        data.putString(TAG_LABEL, (label == null || label.isBlank()) ? "Unlabeled Machine" : label);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static String getLabel(ItemStack stack) {
        CompoundTag data = getData(stack);
        if (data == null || !data.contains(TAG_LABEL)) {
            return "";
        }
        return data.getString(TAG_LABEL).orElse("");
    }

    public static String getCardId(ItemStack stack) {
        CompoundTag data = getData(stack);
        if (data == null || !data.contains(TAG_CARD_ID)) {
            return "";
        }
        return data.getString(TAG_CARD_ID).orElse("");
    }

    private static CompoundTag getData(ItemStack stack) {
        if (!isMachineCard(stack)) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static CompoundTag getDataOrCreate(ItemStack stack) {
        CompoundTag data = getData(stack);
        return data == null ? new CompoundTag() : data;
    }
}
