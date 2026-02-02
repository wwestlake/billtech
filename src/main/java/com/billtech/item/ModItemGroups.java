package com.billtech.item;

import com.billtech.BillTech;
import com.billtech.block.ModBlocks;
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
                        entries.accept(ModBlocks.PUMP);
                        entries.accept(ModBlocks.VALVE);
                        entries.accept(ModBlocks.CHECK_VALVE);
                        entries.accept(ModBlocks.FLOW_METER);
                        entries.accept(ModBlocks.REGULATOR);
                        entries.accept(ModBlocks.TANK_BLOCK);
                        entries.accept(ModBlocks.TANK_CONTROLLER);
                    })
                    .build()
    );

    private ModItemGroups() {
    }

    public static void registerModItemGroups() {
        BillTech.LOGGER.info("Registering Mod Item Groups for " + BillTech.MOD_ID);
    }
}
