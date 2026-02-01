package com.billtech.block;

import com.billtech.BillTech;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class ModBlocks {
    public static final Block FLUID_PIPE = registerBlock(
            "fluid_pipe",
            props -> new FluidPipeBlock(props.strength(1.0f).noOcclusion())
    );
    public static final Block COPPER_WATER_PIPE = registerBlock(
            "copper_water_pipe",
            props -> new FluidPipeBlock(props.strength(1.0f).noOcclusion())
    );

    private ModBlocks() {
    }

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id));
        Block registered = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(props));
        Item.Properties itemProps = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id));
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(registered, itemProps));
        return registered;
    }
}
