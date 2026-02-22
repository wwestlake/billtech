package com.billtech.block;

import com.billtech.BillTech;
import com.billtech.block.entity.FluidPipeBlockEntity;
import com.billtech.block.entity.EnergyCableBlockEntity;
import com.billtech.block.entity.BasicCombustionGeneratorBlockEntity;
import com.billtech.block.entity.FlowMeterBlockEntity;
import com.billtech.block.entity.GasPipeBlockEntity;
import com.billtech.block.entity.ItemPipeBlockEntity;
import com.billtech.block.entity.MethaneCollectorBlockEntity;
import com.billtech.block.entity.MethaneGeneratorBlockEntity;
import com.billtech.block.entity.MethaneTankBlockEntity;
import com.billtech.block.entity.SteamBoilerBlockEntity;
import com.billtech.block.entity.SteamEngineBlockEntity;
import com.billtech.block.entity.SteamGeneratorBlockEntity;
import com.billtech.block.entity.ItemControllerBlockEntity;
import com.billtech.block.entity.AutoCrafterBlockEntity;
import com.billtech.block.entity.RecipeEncoderBlockEntity;
import com.billtech.block.entity.PumpBlockEntity;
import com.billtech.block.entity.ElectricFurnaceBlockEntity;
import com.billtech.block.entity.CoalPyrolyzerBlockEntity;
import com.billtech.block.entity.ReactorBlockEntity;
import com.billtech.block.entity.OilExtractorBlockEntity;
import com.billtech.block.entity.DistillerBlockEntity;
import com.billtech.block.entity.CrackingTowerControllerBlockEntity;
import com.billtech.block.entity.PaperPressBlockEntity;
import com.billtech.block.entity.SeparatorBlockEntity;
import com.billtech.block.entity.RegulatorBlockEntity;
import com.billtech.block.entity.StripeBenchBlockEntity;
import com.billtech.block.entity.TankBlockEntity;
import com.billtech.block.entity.TankControllerBlockEntity;
import com.billtech.block.entity.EssenceExtractorBlockEntity;
import com.billtech.block.entity.TeslaCoilBlockEntity;
import com.billtech.block.entity.ControlConductorPadBlockEntity;
import com.billtech.block.entity.SentryControllerBlockEntity;
import com.billtech.block.entity.SentryContainerBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
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
                    ModBlocks.FLUID_PIPE
            ).build()
    );
    public static final BlockEntityType<GasPipeBlockEntity> GAS_PIPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "gas_pipe"),
            FabricBlockEntityTypeBuilder.create(
                    GasPipeBlockEntity::new,
                    ModBlocks.GAS_PIPE
            ).build()
    );
    public static final BlockEntityType<ItemPipeBlockEntity> ITEM_PIPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "item_pipe"),
            FabricBlockEntityTypeBuilder.create(
                    ItemPipeBlockEntity::new,
                    ModBlocks.ITEM_PIPE
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
    public static final BlockEntityType<EnergyCableBlockEntity> ENERGY_CABLE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "insulated_copper_cable"),
            FabricBlockEntityTypeBuilder.create(
                    EnergyCableBlockEntity::new,
                    ModBlocks.INSULATED_COPPER_CABLE,
                    ModBlocks.CLOTH_INSULATED_COPPER_CABLE,
                    ModBlocks.HV_SHIELDED_CABLE
            ).build()
    );
    public static final BlockEntityType<BasicCombustionGeneratorBlockEntity> BASIC_COMBUSTION_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "basic_combustion_generator"),
            FabricBlockEntityTypeBuilder.create(
                    BasicCombustionGeneratorBlockEntity::new,
                    ModBlocks.BASIC_COMBUSTION_GENERATOR
            ).build()
    );
    public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "electric_furnace"),
            FabricBlockEntityTypeBuilder.create(
                    ElectricFurnaceBlockEntity::new,
                    ModBlocks.ELECTRIC_FURNACE
            ).build()
    );
    public static final BlockEntityType<CoalPyrolyzerBlockEntity> COAL_PYROLYZER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "coal_pyrolyzer"),
            FabricBlockEntityTypeBuilder.create(
                    CoalPyrolyzerBlockEntity::new,
                    ModBlocks.COAL_PYROLYZER
            ).build()
    );
    public static final BlockEntityType<OilExtractorBlockEntity> OIL_EXTRACTOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "oil_extractor"),
            FabricBlockEntityTypeBuilder.create(
                    OilExtractorBlockEntity::new,
                    ModBlocks.OIL_EXTRACTOR
            ).build()
    );
    public static final BlockEntityType<ReactorBlockEntity> REACTOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "reactor"),
            FabricBlockEntityTypeBuilder.create(
                    ReactorBlockEntity::new,
                    ModBlocks.REACTOR
            ).build()
    );
    public static final BlockEntityType<DistillerBlockEntity> DISTILLER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "distiller"),
            FabricBlockEntityTypeBuilder.create(
                    DistillerBlockEntity::new,
                    ModBlocks.DISTILLER
            ).build()
    );
    public static final BlockEntityType<CrackingTowerControllerBlockEntity> CRACKING_TOWER_CONTROLLER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "cracking_tower_controller"),
            FabricBlockEntityTypeBuilder.create(
                    CrackingTowerControllerBlockEntity::new,
                    ModBlocks.CRACKING_TOWER_CONTROLLER
            ).build()
    );
    public static final BlockEntityType<PaperPressBlockEntity> PAPER_PRESS = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "paper_press"),
            FabricBlockEntityTypeBuilder.create(
                    PaperPressBlockEntity::new,
                    ModBlocks.PAPER_PRESS
            ).build()
    );
    public static final BlockEntityType<SeparatorBlockEntity> SEPARATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "separator"),
            FabricBlockEntityTypeBuilder.create(
                    SeparatorBlockEntity::new,
                    ModBlocks.SEPARATOR
            ).build()
    );
    public static final BlockEntityType<StripeBenchBlockEntity> STRIPE_BENCH = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "stripe_bench"),
            FabricBlockEntityTypeBuilder.create(
                    StripeBenchBlockEntity::new,
                    ModBlocks.STRIPE_BENCH
            ).build()
    );
    public static final BlockEntityType<MethaneCollectorBlockEntity> METHANE_COLLECTOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_collector"),
            FabricBlockEntityTypeBuilder.create(
                    MethaneCollectorBlockEntity::new,
                    ModBlocks.METHANE_COLLECTOR
            ).build()
    );
    public static final BlockEntityType<MethaneGeneratorBlockEntity> METHANE_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_generator"),
            FabricBlockEntityTypeBuilder.create(
                    MethaneGeneratorBlockEntity::new,
                    ModBlocks.METHANE_GENERATOR
            ).build()
    );
    public static final BlockEntityType<MethaneTankBlockEntity> METHANE_TANK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_tank"),
            FabricBlockEntityTypeBuilder.create(
                    MethaneTankBlockEntity::new,
                    ModBlocks.METHANE_TANK
            ).build()
    );
    public static final BlockEntityType<SteamBoilerBlockEntity> STEAM_BOILER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "steam_boiler"),
            FabricBlockEntityTypeBuilder.create(
                    SteamBoilerBlockEntity::new,
                    ModBlocks.STEAM_BOILER
            ).build()
    );
    public static final BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "steam_engine"),
            FabricBlockEntityTypeBuilder.create(
                    SteamEngineBlockEntity::new,
                    ModBlocks.STEAM_ENGINE
            ).build()
    );
    public static final BlockEntityType<SteamGeneratorBlockEntity> STEAM_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "steam_generator"),
            FabricBlockEntityTypeBuilder.create(
                    SteamGeneratorBlockEntity::new,
                    ModBlocks.STEAM_GENERATOR
            ).build()
    );
    public static final BlockEntityType<EssenceExtractorBlockEntity> ESSENCE_EXTRACTOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "essence_extractor"),
            FabricBlockEntityTypeBuilder.create(
                    EssenceExtractorBlockEntity::new,
                    ModBlocks.ESSENCE_EXTRACTOR
            ).build()
    );
    public static final BlockEntityType<TeslaCoilBlockEntity> TESLA_COIL = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "tesla_coil"),
            FabricBlockEntityTypeBuilder.create(
                    TeslaCoilBlockEntity::new,
                    ModBlocks.TESLA_COIL
            ).build()
    );
    public static final BlockEntityType<ControlConductorPadBlockEntity> CONTROL_CONDUCTOR_PAD = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "control_conductor_pad"),
            FabricBlockEntityTypeBuilder.create(
                    ControlConductorPadBlockEntity::new,
                    ModBlocks.CONTROL_CONDUCTOR_PAD
            ).build()
    );
    public static final BlockEntityType<SentryControllerBlockEntity> SENTRY_CONTROLLER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "sentry_controller"),
            FabricBlockEntityTypeBuilder.create(
                    SentryControllerBlockEntity::new,
                    ModBlocks.SENTRY_CONTROLLER
            ).build()
    );
    public static final BlockEntityType<SentryContainerBlockEntity> SENTRY_CONTAINER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "sentry_container"),
            FabricBlockEntityTypeBuilder.create(
                    SentryContainerBlockEntity::new,
                    ModBlocks.SENTRY_CONTAINER
            ).build()
    );
    public static final BlockEntityType<ItemControllerBlockEntity> ITEM_CONTROLLER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "item_controller"),
            FabricBlockEntityTypeBuilder.create(
                    ItemControllerBlockEntity::new,
                    ModBlocks.ITEM_CONTROLLER
            ).build()
    );
    public static final BlockEntityType<RecipeEncoderBlockEntity> RECIPE_ENCODER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "recipe_encoder"),
            FabricBlockEntityTypeBuilder.create(
                    RecipeEncoderBlockEntity::new,
                    ModBlocks.RECIPE_ENCODER
            ).build()
    );
    public static final BlockEntityType<AutoCrafterBlockEntity> AUTO_CRAFTER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "auto_crafter"),
            FabricBlockEntityTypeBuilder.create(
                    AutoCrafterBlockEntity::new,
                    ModBlocks.AUTO_CRAFTER
            ).build()
    );

    private ModBlockEntities() {
    }

    static {
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getNetworkStorage(), TANK_BLOCK);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(direction), FLUID_PIPE);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(direction), GAS_PIPE);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getStorage(direction),
                ITEM_PIPE
        );
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> InventoryStorage.of(be, direction),
                ESSENCE_EXTRACTOR
        );
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(direction), PUMP);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), COAL_PYROLYZER);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), OIL_EXTRACTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), REACTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), DISTILLER);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), CRACKING_TOWER_CONTROLLER);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), METHANE_COLLECTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), METHANE_GENERATOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getStorage(), METHANE_TANK);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), STEAM_BOILER);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), STEAM_ENGINE);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), STEAM_GENERATOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), ESSENCE_EXTRACTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.getFluidStorage(direction), SENTRY_CONTROLLER);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                BASIC_COMBUSTION_GENERATOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                ELECTRIC_FURNACE
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                COAL_PYROLYZER
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                OIL_EXTRACTOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                REACTOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                DISTILLER
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                CRACKING_TOWER_CONTROLLER
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                PAPER_PRESS
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                SEPARATOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                METHANE_GENERATOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                STEAM_BOILER
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                STEAM_ENGINE
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                STEAM_GENERATOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                ESSENCE_EXTRACTOR
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                TESLA_COIL
        );
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.getEnergyStorage(direction),
                SENTRY_CONTROLLER
        );
    }
}
