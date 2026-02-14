package com.billtech.stripe;

import net.minecraft.nbt.CompoundTag;

/**
 * Stripe payload stored on stripe-capable pipes/cables.
 * Holds up to three dye color indices and a packed signature for fast equality checks.
 */
public final class StripeData {
    public static final StripeData EMPTY = new StripeData(0, new int[]{0, 0, 0});

    private final int stripeCount;
    private final int[] colors; // length 3, values 0-15 (vanilla dye indices)
    private final int signature;

    public StripeData(int stripeCount, int[] colors) {
        int count = Math.max(0, Math.min(3, stripeCount));
        int[] safe = new int[]{0, 0, 0};
        if (colors != null) {
            for (int i = 0; i < Math.min(colors.length, 3); i++) {
                safe[i] = clampColor(colors[i]);
            }
        }
        this.stripeCount = count;
        this.colors = safe;
        this.signature = packSignature(count, safe);
    }

    public int stripeCount() {
        return stripeCount;
    }

    public int[] colors() {
        return new int[]{colors[0], colors[1], colors[2]};
    }

    public int signature() {
        return signature;
    }

    public boolean isEmpty() {
        return stripeCount == 0;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("StripeCount", stripeCount);
        tag.putIntArray("StripeColors", colors);
        return tag;
    }

    public static StripeData load(CompoundTag tag) {
        if (tag == null || !tag.contains("StripeCount")) {
            return EMPTY;
        }
        int count = tag.getInt("StripeCount").orElse(0);
        int[] loaded = tag.getIntArray("StripeColors").orElse(new int[0]);
        return new StripeData(count, loaded);
    }

    private static int packSignature(int count, int[] cols) {
        // 2 bits for count, 3 * 4-bit color indices.
        int sig = (count & 0x3) << 12;
        sig |= (cols[0] & 0xF) << 8;
        sig |= (cols[1] & 0xF) << 4;
        sig |= (cols[2] & 0xF);
        return sig;
    }

    private static int clampColor(int value) {
        if (value < 0) return 0;
        if (value > 15) return 15;
        return value;
    }
}
