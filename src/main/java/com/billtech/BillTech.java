package com.billtech;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.ModBlocks;
import com.billtech.command.ModCommands;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.item.ModItems;
import com.billtech.item.ModItemGroups;
import com.billtech.menu.ModMenus;
import com.billtech.menu.ItemControllerMenu;
import com.billtech.network.ItemControllerSearchPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillTech implements ModInitializer {
	public static final String MOD_ID = "billtech";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BillTechConfig.load();
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Registering Items");
		ModItems.registerModItems();
		ModItemGroups.registerModItemGroups();
		LOGGER.info("Registering Blocks");
		// Touch classes to ensure static init runs.
		ModBlocks.FLUID_PIPE.toString();
		ModBlockEntities.FLUID_PIPE.toString();
		ModFluids.init();
		ModMenus.TANK_CONTROLLER.toString();
		ModCommands.register();
		registerNetworking();

		// Custom BillTech creative tab is registered in ModItemGroups.


	}

	private void registerNetworking() {
		PayloadTypeRegistry.playC2S().register(ItemControllerSearchPayload.TYPE, ItemControllerSearchPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(
				ItemControllerSearchPayload.TYPE,
				(payload, context) -> context.player().server.execute(() -> {
					if (context.player().containerMenu instanceof ItemControllerMenu menu) {
						menu.setSearchText(payload.query());
					}
				})
		);
	}
}
