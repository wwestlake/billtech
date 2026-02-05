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
    public static final Block GAS_PIPE = registerBlock(
            "gas_pipe",
            props -> new GasPipeBlock(props.strength(1.0f).noOcclusion())
    );
    public static final Block TANK_BLOCK = registerBlock(
            "tank_block",
            props -> new TankBlock(props.strength(2.0f).noOcclusion())
    );
    public static final Block TANK_CONTROLLER = registerBlock(
            "tank_controller",
            props -> new TankControllerBlock(props.strength(2.0f))
    );
    public static final Block PUMP = registerBlock(
            "pump",
            props -> new PumpBlock(props.strength(2.0f))
    );
    public static final Block VALVE = registerBlock(
            "valve",
            props -> new ValveBlock(props.strength(2.0f))
    );
    public static final Block CHECK_VALVE = registerBlock(
            "check_valve",
            props -> new CheckValveBlock(props.strength(2.0f))
    );
    public static final Block FLOW_METER = registerBlock(
            "flow_meter",
            props -> new FlowMeterBlock(props.strength(2.0f))
    );
    public static final Block REGULATOR = registerBlock(
            "regulator",
            props -> new RegulatorBlock(props.strength(2.0f))
    );
    public static final Block INSULATED_COPPER_CABLE = registerBlock(
            "insulated_copper_cable",
            props -> new EnergyCableBlock(props.strength(1.0f).noOcclusion())
    );
    public static final Block CLOTH_INSULATED_COPPER_CABLE = registerBlock(
            "cloth_insulated_copper_cable",
            props -> new EnergyCableBlock(props.strength(1.0f).noOcclusion())
    );
    public static final Block BASIC_COMBUSTION_GENERATOR = registerBlock(
            "basic_combustion_generator",
            props -> new BasicCombustionGeneratorBlock(props.strength(2.0f))
    );
    public static final Block ELECTRIC_FURNACE = registerBlock(
            "electric_furnace",
            props -> new ElectricFurnaceBlock(props.strength(2.0f))
    );
    public static final Block COAL_PYROLYZER = registerBlock(
            "coal_pyrolyzer",
            props -> new CoalPyrolyzerBlock(props.strength(2.0f))
    );
    public static final Block OIL_EXTRACTOR = registerBlock(
            "oil_extractor",
            props -> new OilExtractorBlock(props.strength(2.0f))
    );
    public static final Block DISTILLER = registerBlock(
            "distiller",
            props -> new DistillerBlock(props.strength(2.0f))
    );
    public static final Block PAPER_PRESS = registerBlock(
            "paper_press",
            props -> new PaperPressBlock(props.strength(2.0f))
    );
    public static final Block METHANE_COLLECTOR = registerBlock(
            "methane_collector",
            props -> new MethaneCollectorBlock(props.strength(2.0f))
    );
    public static final Block METHANE_TANK = registerBlock(
            "methane_tank",
            props -> new MethaneTankBlock(props.strength(2.0f))
    );
    public static final Block METHANE_GENERATOR = registerBlock(
            "methane_generator",
            props -> new MethaneGeneratorBlock(props.strength(2.0f))
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
