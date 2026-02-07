package com.billtech;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import com.billtech.block.ModBlocks;
import com.billtech.block.ModBlockEntities;
import com.billtech.client.render.FlowMeterBlockEntityRenderer;
import com.billtech.client.render.PipeCoverBlockEntityRenderer;
import com.billtech.client.render.TankBlockEntityRenderer;
import com.billtech.menu.ModMenus;
import com.billtech.client.screen.BasicCombustionGeneratorScreen;
import com.billtech.client.screen.ReactorScreen;
import com.billtech.client.screen.CoalPyrolyzerScreen;
import com.billtech.client.screen.CrackingTowerScreen;
import com.billtech.client.screen.DistillerScreen;
import com.billtech.client.screen.ElectricFurnaceScreen;
import com.billtech.client.screen.MethaneCollectorScreen;
import com.billtech.client.screen.MethaneGeneratorScreen;
import com.billtech.client.screen.MethaneTankScreen;
import com.billtech.client.screen.OilExtractorScreen;
import com.billtech.client.screen.PaperPressScreen;
import com.billtech.client.screen.RegulatorScreen;
import com.billtech.client.screen.SeparatorScreen;
import com.billtech.client.screen.TankControllerScreen;
import com.billtech.client.screen.UpgradeScreen;
import com.billtech.client.screen.ItemControllerScreen;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;

public class BillTechClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COPPER_WATER_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLUID_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GAS_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ITEM_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INSULATED_COPPER_CABLE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TANK_BLOCK, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TANK_CONTROLLER, RenderType.cutout());
		MenuScreens.register(ModMenus.TANK_CONTROLLER, TankControllerScreen::new);
		MenuScreens.register(ModMenus.UPGRADES, UpgradeScreen::new);
		MenuScreens.register(ModMenus.REGULATOR, RegulatorScreen::new);
		MenuScreens.register(ModMenus.BASIC_COMBUSTION_GENERATOR, BasicCombustionGeneratorScreen::new);
		MenuScreens.register(ModMenus.ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
		MenuScreens.register(ModMenus.COAL_PYROLYZER, CoalPyrolyzerScreen::new);
		MenuScreens.register(ModMenus.OIL_EXTRACTOR, OilExtractorScreen::new);
		MenuScreens.register(ModMenus.REACTOR, ReactorScreen::new);
		MenuScreens.register(ModMenus.DISTILLER, DistillerScreen::new);
		MenuScreens.register(ModMenus.CRACKING_TOWER, CrackingTowerScreen::new);
		MenuScreens.register(ModMenus.PAPER_PRESS, PaperPressScreen::new);
		MenuScreens.register(ModMenus.SEPARATOR, SeparatorScreen::new);
		MenuScreens.register(ModMenus.METHANE_COLLECTOR, MethaneCollectorScreen::new);
		MenuScreens.register(ModMenus.METHANE_GENERATOR, MethaneGeneratorScreen::new);
		MenuScreens.register(ModMenus.METHANE_TANK, MethaneTankScreen::new);
		MenuScreens.register(ModMenus.ITEM_CONTROLLER, ItemControllerScreen::new);
		registerFluidRenders();
		BlockEntityRendererRegistry.register(ModBlockEntities.TANK_BLOCK, TankBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.FLOW_METER, FlowMeterBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.FLUID_PIPE, PipeCoverBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.GAS_PIPE, PipeCoverBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.ITEM_PIPE, PipeCoverBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.ENERGY_CABLE, PipeCoverBlockEntityRenderer::new);
	}

	private static void registerFluidRenders() {
		int color = 0xFFFFFFFF;
		registerFluid(ModFluids.COAL_OIL, ModFluids.COAL_OIL_FLOWING, "coal_oil", color);
		registerFluid(ModFluids.CRUDE_OIL, ModFluids.CRUDE_OIL_FLOWING, "crude_oil", color);
		registerFluid(ModFluids.SLUDGE, ModFluids.SLUDGE_FLOWING, "sludge", color);
		registerFluid(ModFluids.LIGHT_FUEL, ModFluids.LIGHT_FUEL_FLOWING, "light_fuel", color);
		registerFluid(ModFluids.HEAVY_FUEL, ModFluids.HEAVY_FUEL_FLOWING, "heavy_fuel", color);
		registerFluid(ModFluids.METHANE, ModFluids.METHANE_FLOWING, "methane", color);
		registerFluid(ModFluids.SULFURIC_ACID, ModFluids.SULFURIC_ACID_FLOWING, "sulfuric_acid", color);
		registerFluid(ModFluids.HYDROCHLORIC_ACID, ModFluids.HYDROCHLORIC_ACID_FLOWING, "hydrochloric_acid", color);
		registerFluid(ModFluids.PHOSPHORIC_ACID, ModFluids.PHOSPHORIC_ACID_FLOWING, "phosphoric_acid", color);
		registerFluid(ModFluids.SODIUM_HYDROXIDE, ModFluids.SODIUM_HYDROXIDE_FLOWING, "sodium_hydroxide", color);
		registerFluid(ModFluids.LIMEWATER, ModFluids.LIMEWATER_FLOWING, "limewater", color);
		registerFluid(ModFluids.LIGHT_FRACTION, ModFluids.LIGHT_FRACTION_FLOWING, "light_fraction", color);
		registerFluid(ModFluids.MEDIUM_FRACTION, ModFluids.MEDIUM_FRACTION_FLOWING, "medium_fraction", color);
		registerFluid(ModFluids.HEAVY_FRACTION, ModFluids.HEAVY_FRACTION_FLOWING, "heavy_fraction", color);
		registerFluid(ModFluids.RESIDUE, ModFluids.RESIDUE_FLOWING, "residue", color);
	}

	private static void registerFluid(net.minecraft.world.level.material.Fluid still, net.minecraft.world.level.material.Fluid flowing, String name, int color) {
		ResourceLocation stillTex = ResourceLocation.fromNamespaceAndPath("billtech", "block/" + name + "_still");
		ResourceLocation flowTex = ResourceLocation.fromNamespaceAndPath("billtech", "block/" + name + "_flow");
		FluidRenderHandlerRegistry.INSTANCE.register(still, flowing, new SimpleFluidRenderHandler(stillTex, flowTex) {
			@Override
			public int getFluidColor(net.minecraft.world.level.BlockAndTintGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.level.material.FluidState state) {
				return color;
			}
		});
	}
}
