package com.billtech.item;

import com.billtech.BillTech;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item BILLTECH_BUCKET = registerBucket("billtech_bucket");
    public static final Item PLASTIC_SHEET = registerSimpleItem("plastic_sheet");
    public static final Item RECIPE_CARD = registerSimpleItem("recipe_card");
    public static final Item STRIPE_APPLICATOR = registerStripeApplicator("stripe_applicator");
    public static final Item UPGRADE_BASE = registerSimpleItem("upgrade_base");
    public static final Item STEEL_INGOT = registerSimpleItem("steel_ingot");
    public static final Item SILICA_POWDER = registerSimpleItem("silica_powder");
    public static final Item LIME_POWDER = registerSimpleItem("lime_powder");
    public static final Item QUICKLIME = registerSimpleItem("quicklime");
    public static final Item ALUMINA_POWDER = registerSimpleItem("alumina_powder");
    public static final Item IRON_OXIDE_POWDER = registerSimpleItem("iron_oxide_powder");
    public static final Item SODIUM_SALT = registerSimpleItem("sodium_salt");
    public static final Item SODA_ASH = registerSimpleItem("soda_ash");
    public static final Item SULFUR_POWDER = registerSimpleItem("sulfur_powder");
    public static final Item SLAG = registerSimpleItem("slag");
    public static final Item PIPE_COVER = registerSimpleItem("pipe_cover");
    public static final Item PHOSPHORUS_POWDER = registerSimpleItem("phosphorus_powder");
    public static final Item SPEED_UPGRADE = registerItem("speed_upgrade", UpgradeType.SPEED);
    public static final Item POWER_UPGRADE = registerItem("power_upgrade", UpgradeType.POWER);
    public static final Item ENERGY_UPGRADE = registerItem("energy_upgrade", UpgradeType.ENERGY);
    public static final Item STORAGE_UPGRADE = registerItem("storage_upgrade", UpgradeType.STORAGE);

    public static void registerModItems() {
        BillTech.LOGGER.info("Registering Mod Items for " + BillTech.MOD_ID);
    }

    private static Item registerItem(String name, UpgradeType type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
        return Registry.register(BuiltInRegistries.ITEM, id, new UpgradeItem(props, type));
    }

    private static Item registerSimpleItem(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(props));
    }

    private static Item registerStripeApplicator(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        Item.Properties props = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(1);
        return Registry.register(BuiltInRegistries.ITEM, id, new StripeApplicatorItem(props));
    }

    private static Item registerBucket(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        Item.Properties props = new Item.Properties()
                .stacksTo(1)
                .setId(ResourceKey.create(Registries.ITEM, id));
        return Registry.register(BuiltInRegistries.ITEM, id, new BillTechBucketItem(props));
    }
}
