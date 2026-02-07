package com.billtech.network;

import com.billtech.BillTech;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ItemControllerSearchPayload(String query) implements CustomPacketPayload {
    public static final Type<ItemControllerSearchPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "item_controller_search"));
    public static final StreamCodec<FriendlyByteBuf, ItemControllerSearchPayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> buf.writeUtf(value.query, 64),
                    buf -> new ItemControllerSearchPayload(buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
