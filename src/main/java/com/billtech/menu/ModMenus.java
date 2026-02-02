package com.billtech.menu;

import com.billtech.BillTech;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<TankControllerMenu> TANK_CONTROLLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "tank_controller"),
            new MenuType<>(TankControllerMenu::new, FeatureFlags.VANILLA_SET)
    );

    private ModMenus() {
    }
}
