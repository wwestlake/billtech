package com.billtech.block;

import com.billtech.BillTech;
import com.billtech.block.entity.FluidPipeBlockEntity;
import com.billtech.block.entity.FlowMeterBlockEntity;
import com.billtech.block.entity.PumpBlockEntity;
import com.billtech.block.entity.RegulatorBlockEntity;
import com.billtech.block.entity.TankBlockEntity;
import com.billtech.block.entity.TankControllerBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
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
    public static final BlockEntityType<TankBlockEntity> TANK_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "tank_block"),
            FabricBlockEntityTypeBuilder.create(
                    TankBlockEntity::new,
                    ModBlocks.TANK_BLOCK
            ).build()
    );
    public static final BlockEntityType<TankControllerBlockEntity> TANK_CONTROLLER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "tank_controller"),
            FabricBlockEntityTypeBuilder.create(
                    TankControllerBlockEntity::new,
                    ModBlocks.TANK_CONTROLLER
            ).build()
    );
    public static final BlockEntityType<PumpBlockEntity> PUMP = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "pump"),
            FabricBlockEntityTypeBuilder.create(
                    PumpBlockEntity::new,
                    ModBlocks.PUMP
            ).build()
    );
    public static final BlockEntityType<FlowMeterBlockEntity> FLOW_METER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "flow_meter"),
            FabricBlockEntityTypeBuilder.create(
                    FlowMeterBlockEntity::new,
                    ModBlocks.FLOW_METER
            ).build()
    );
    public static final BlockEntityType<RegulatorBlockEntity> REGULATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "regulator"),
            FabricBlockEntityTypeBuilder.create(
                    RegulatorBlockEntity::new,
                    ModBlocks.REGULATOR
            ).build()
    );

    private ModBlockEntities() {
    }

    static {
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getNetworkStorage(), TANK_BLOCK);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(direction), FLUID_PIPE);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(direction), PUMP);
    }
}
