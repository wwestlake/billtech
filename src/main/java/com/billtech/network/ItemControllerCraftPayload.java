package com.billtech.network;

import com.billtech.BillTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ItemControllerCraftPayload(ItemStack stack, int amount) implements CustomPacketPayload {
    public static final Type<ItemControllerCraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "item_controller_craft"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemControllerCraftPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, ItemControllerCraftPayload::stack,
                    ByteBufCodecs.VAR_INT, ItemControllerCraftPayload::amount,
                    ItemControllerCraftPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
