package com.billtech.menu;

import com.billtech.BillTech;
import com.billtech.menu.UpgradeMenu;
import com.billtech.menu.RegulatorMenu;
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
    public static final MenuType<UpgradeMenu> UPGRADES = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "upgrades"),
            new MenuType<>(UpgradeMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<RegulatorMenu> REGULATOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "regulator"),
            new MenuType<>(RegulatorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<BasicCombustionGeneratorMenu> BASIC_COMBUSTION_GENERATOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "basic_combustion_generator"),
            new MenuType<>(BasicCombustionGeneratorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<ElectricFurnaceMenu> ELECTRIC_FURNACE = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "electric_furnace"),
            new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<CoalPyrolyzerMenu> COAL_PYROLYZER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "coal_pyrolyzer"),
            new MenuType<>(CoalPyrolyzerMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<OilExtractorMenu> OIL_EXTRACTOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "oil_extractor"),
            new MenuType<>(OilExtractorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<ReactorMenu> REACTOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "reactor"),
            new MenuType<>(ReactorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<DistillerMenu> DISTILLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "distiller"),
            new MenuType<>(DistillerMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<CrackingTowerMenu> CRACKING_TOWER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "cracking_tower"),
            new MenuType<>(CrackingTowerMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<PaperPressMenu> PAPER_PRESS = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "paper_press"),
            new MenuType<>(PaperPressMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<SeparatorMenu> SEPARATOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "separator"),
            new MenuType<>(SeparatorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<MethaneCollectorMenu> METHANE_COLLECTOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_collector"),
            new MenuType<>(MethaneCollectorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<MethaneGeneratorMenu> METHANE_GENERATOR = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_generator"),
            new MenuType<>(MethaneGeneratorMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<MethaneTankMenu> METHANE_TANK = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_tank"),
            new MenuType<>(MethaneTankMenu::new, FeatureFlags.VANILLA_SET)
    );
    public static final MenuType<ItemControllerMenu> ITEM_CONTROLLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "item_controller"),
            new MenuType<>(ItemControllerMenu::new, FeatureFlags.VANILLA_SET)
    );

    private ModMenus() {
    }
}
