package com.billtech.block;

import com.billtech.BillTech;
import com.billtech.block.entity.FluidPipeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<FluidPipeBlockEntity> FLUID_PIPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "fluid_pipe"),
            FabricBlockEntityTypeBuilder.create(
                    FluidPipeBlockEntity::new,
                    ModBlocks.FLUID_PIPE,
                    ModBlocks.COPPER_WATER_PIPE
            ).build()
    );

    private ModBlockEntities() {
    }
}
