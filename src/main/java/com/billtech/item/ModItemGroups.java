package com.billtech.item;

import com.billtech.BillTech;
import com.billtech.block.ModBlocks;
import com.billtech.item.ModItems;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
    public static final CreativeModeTab BILLTECH = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "billtech"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.billtech"))
                    .icon(() -> new ItemStack(ModBlocks.PUMP))
                    .displayItems((ctx, entries) -> {
                        entries.accept(ModBlocks.FLUID_PIPE);
                        entries.accept(ModBlocks.COPPER_WATER_PIPE);
                        entries.accept(ModBlocks.GAS_PIPE);
                        entries.accept(ModBlocks.ITEM_PIPE);
                        entries.accept(ModBlocks.PUMP);
                        entries.accept(ModBlocks.VALVE);
                        entries.accept(ModBlocks.CHECK_VALVE);
                        entries.accept(ModBlocks.FLOW_METER);
                        entries.accept(ModBlocks.REGULATOR);
                        entries.accept(ModBlocks.INSULATED_COPPER_CABLE);
                        entries.accept(ModBlocks.CLOTH_INSULATED_COPPER_CABLE);
                        entries.accept(ModBlocks.BASIC_COMBUSTION_GENERATOR);
                        entries.accept(ModBlocks.ELECTRIC_FURNACE);
                        entries.accept(ModBlocks.COAL_PYROLYZER);
                        entries.accept(ModBlocks.OIL_EXTRACTOR);
                        entries.accept(ModBlocks.REACTOR);
                        entries.accept(ModBlocks.DISTILLER);
                        entries.accept(ModBlocks.CRACKING_TOWER_CONTROLLER);
                        entries.accept(ModBlocks.CRACKING_TOWER);
                        entries.accept(ModBlocks.PAPER_PRESS);
                        entries.accept(ModBlocks.GRINDER);
                        entries.accept(ModBlocks.SEPARATOR);
                        entries.accept(ModBlocks.METHANE_COLLECTOR);
                        entries.accept(ModBlocks.METHANE_TANK);
                        entries.accept(ModBlocks.METHANE_GENERATOR);
                        entries.accept(ModBlocks.ITEM_CONTROLLER);
                        entries.accept(ModBlocks.RECIPE_ENCODER);
                        entries.accept(ModBlocks.AUTO_CRAFTER);
                        entries.accept(ModBlocks.TANK_BLOCK);
                        entries.accept(ModBlocks.TANK_CONTROLLER);
                        entries.accept(ModItems.BILLTECH_BUCKET);
                        entries.accept(ModItems.PLASTIC_SHEET);
                        entries.accept(ModItems.RECIPE_CARD);
                        entries.accept(ModFluids.SLUDGE_BUCKET);
                        entries.accept(ModFluids.CRUDE_OIL_BUCKET);
                        entries.accept(ModFluids.LIGHT_FUEL_BUCKET);
                        entries.accept(ModFluids.HEAVY_FUEL_BUCKET);
                        entries.accept(ModFluids.METHANE_BUCKET);
                        entries.accept(ModFluids.SULFURIC_ACID_BUCKET);
                        entries.accept(ModFluids.HYDROCHLORIC_ACID_BUCKET);
                        entries.accept(ModFluids.PHOSPHORIC_ACID_BUCKET);
                        entries.accept(ModFluids.SODIUM_HYDROXIDE_BUCKET);
                        entries.accept(ModFluids.LIMEWATER_BUCKET);
                        entries.accept(ModFluids.LIGHT_FRACTION_BUCKET);
                        entries.accept(ModFluids.MEDIUM_FRACTION_BUCKET);
                        entries.accept(ModFluids.HEAVY_FRACTION_BUCKET);
                        entries.accept(ModFluids.RESIDUE_BUCKET);
                        entries.accept(ModItems.SILICA_POWDER);
                        entries.accept(ModItems.LIME_POWDER);
                        entries.accept(ModItems.QUICKLIME);
                        entries.accept(ModItems.ALUMINA_POWDER);
                        entries.accept(ModItems.IRON_OXIDE_POWDER);
                        entries.accept(ModItems.SODIUM_SALT);
                        entries.accept(ModItems.SODA_ASH);
                        entries.accept(ModItems.SULFUR_POWDER);
                        entries.accept(ModItems.SLAG);
                        entries.accept(ModItems.PIPE_COVER);
                        entries.accept(ModItems.PHOSPHORUS_POWDER);
                        entries.accept(ModItems.SPEED_UPGRADE);
                        entries.accept(ModItems.POWER_UPGRADE);
                        entries.accept(ModItems.ENERGY_UPGRADE);
                        entries.accept(ModItems.STORAGE_UPGRADE);
                    })
                    .build()
    );

    private ModItemGroups() {
    }

    public static void registerModItemGroups() {
        BillTech.LOGGER.info("Registering Mod Item Groups for " + BillTech.MOD_ID);
    }
}
