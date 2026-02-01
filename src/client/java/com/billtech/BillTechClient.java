package com.billtech;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import com.billtech.block.ModBlocks;

public class BillTechClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COPPER_WATER_PIPE, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLUID_PIPE, RenderType.cutout());
	}
}
