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
import com.billtech.client.screen.CoalPyrolyzerScreen;
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
		MenuScreens.register(ModMenus.DISTILLER, DistillerScreen::new);
		MenuScreens.register(ModMenus.PAPER_PRESS, PaperPressScreen::new);
		MenuScreens.register(ModMenus.SEPARATOR, SeparatorScreen::new);
		MenuScreens.register(ModMenus.METHANE_COLLECTOR, MethaneCollectorScreen::new);
		MenuScreens.register(ModMenus.METHANE_GENERATOR, MethaneGeneratorScreen::new);
		MenuScreens.register(ModMenus.METHANE_TANK, MethaneTankScreen::new);
		registerFluidRenders();
		BlockEntityRendererRegistry.register(ModBlockEntities.TANK_BLOCK, TankBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.FLOW_METER, FlowMeterBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.FLUID_PIPE, PipeCoverBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.GAS_PIPE, PipeCoverBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.ENERGY_CABLE, PipeCoverBlockEntityRenderer::new);
	}

	private static void registerFluidRenders() {
		registerFluid(ModFluids.COAL_OIL, ModFluids.COAL_OIL_FLOWING, "coal_oil");
		registerFluid(ModFluids.CRUDE_OIL, ModFluids.CRUDE_OIL_FLOWING, "crude_oil");
		registerFluid(ModFluids.SLUDGE, ModFluids.SLUDGE_FLOWING, "sludge");
		registerFluid(ModFluids.LIGHT_FUEL, ModFluids.LIGHT_FUEL_FLOWING, "light_fuel");
		registerFluid(ModFluids.HEAVY_FUEL, ModFluids.HEAVY_FUEL_FLOWING, "heavy_fuel");
		registerFluid(ModFluids.METHANE, ModFluids.METHANE_FLOWING, "methane");
	}

	private static void registerFluid(net.minecraft.world.level.material.Fluid still, net.minecraft.world.level.material.Fluid flowing, String name) {
		ResourceLocation stillTex = ResourceLocation.fromNamespaceAndPath("billtech", "block/" + name + "_still");
		ResourceLocation flowTex = ResourceLocation.fromNamespaceAndPath("billtech", "block/" + name + "_flow");
		FluidRenderHandlerRegistry.INSTANCE.register(still, flowing, new SimpleFluidRenderHandler(stillTex, flowTex));
	}
}
