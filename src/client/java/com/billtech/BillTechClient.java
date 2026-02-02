package com.billtech;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import com.billtech.block.ModBlocks;
import com.billtech.block.ModBlockEntities;
import com.billtech.client.render.FlowMeterBlockEntityRenderer;
import com.billtech.client.render.TankBlockEntityRenderer;
import com.billtech.menu.ModMenus;
import com.billtech.client.screen.TankControllerScreen;

public class BillTechClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COPPER_WATER_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLUID_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TANK_BLOCK, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TANK_CONTROLLER, RenderType.cutout());
		MenuScreens.register(ModMenus.TANK_CONTROLLER, TankControllerScreen::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.TANK_BLOCK, TankBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(ModBlockEntities.FLOW_METER, FlowMeterBlockEntityRenderer::new);
	}
}
