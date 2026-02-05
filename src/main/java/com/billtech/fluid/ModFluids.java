package com.billtech.fluid;

import com.billtech.BillTech;
import com.billtech.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class ModFluids {
    public static FlowingFluid SLUDGE;
    public static FlowingFluid SLUDGE_FLOWING;

    public static FlowingFluid COAL_OIL;
    public static FlowingFluid COAL_OIL_FLOWING;

    public static FlowingFluid CRUDE_OIL;
    public static FlowingFluid CRUDE_OIL_FLOWING;

    public static FlowingFluid LIGHT_FUEL;
    public static FlowingFluid LIGHT_FUEL_FLOWING;

    public static FlowingFluid HEAVY_FUEL;
    public static FlowingFluid HEAVY_FUEL_FLOWING;
    public static FlowingFluid METHANE;
    public static FlowingFluid METHANE_FLOWING;

    public static Block SLUDGE_BLOCK;
    public static Block COAL_OIL_BLOCK;
    public static Block CRUDE_OIL_BLOCK;
    public static Block LIGHT_FUEL_BLOCK;
    public static Block HEAVY_FUEL_BLOCK;
    public static Block METHANE_BLOCK;

    public static Item SLUDGE_BUCKET;
    public static Item COAL_OIL_BUCKET;
    public static Item CRUDE_OIL_BUCKET;
    public static Item LIGHT_FUEL_BUCKET;
    public static Item HEAVY_FUEL_BUCKET;
    public static Item METHANE_BUCKET;

    private ModFluids() {
    }

    public static void init() {
        if (SLUDGE != null) {
            return;
        }

        SLUDGE = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "sludge"),
                new SludgeFluid.Still()
        );
        SLUDGE_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "sludge_flowing"),
                new SludgeFluid.Flowing()
        );

        COAL_OIL = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "coal_oil"),
                new BasicFluid.Still(() -> COAL_OIL, () -> COAL_OIL_FLOWING, () -> COAL_OIL_BLOCK, () -> COAL_OIL_BUCKET)
        );
        COAL_OIL_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "coal_oil_flowing"),
                new BasicFluid.Flowing(() -> COAL_OIL, () -> COAL_OIL_FLOWING, () -> COAL_OIL_BLOCK, () -> COAL_OIL_BUCKET)
        );

        CRUDE_OIL = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "crude_oil"),
                new BasicFluid.Still(() -> CRUDE_OIL, () -> CRUDE_OIL_FLOWING, () -> CRUDE_OIL_BLOCK, () -> CRUDE_OIL_BUCKET)
        );
        CRUDE_OIL_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "crude_oil_flowing"),
                new BasicFluid.Flowing(() -> CRUDE_OIL, () -> CRUDE_OIL_FLOWING, () -> CRUDE_OIL_BLOCK, () -> CRUDE_OIL_BUCKET)
        );

        LIGHT_FUEL = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "light_fuel"),
                new BasicFluid.Still(() -> LIGHT_FUEL, () -> LIGHT_FUEL_FLOWING, () -> LIGHT_FUEL_BLOCK, () -> LIGHT_FUEL_BUCKET)
        );
        LIGHT_FUEL_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "light_fuel_flowing"),
                new BasicFluid.Flowing(() -> LIGHT_FUEL, () -> LIGHT_FUEL_FLOWING, () -> LIGHT_FUEL_BLOCK, () -> LIGHT_FUEL_BUCKET)
        );

        HEAVY_FUEL = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "heavy_fuel"),
                new BasicFluid.Still(() -> HEAVY_FUEL, () -> HEAVY_FUEL_FLOWING, () -> HEAVY_FUEL_BLOCK, () -> HEAVY_FUEL_BUCKET)
        );
        HEAVY_FUEL_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "heavy_fuel_flowing"),
                new BasicFluid.Flowing(() -> HEAVY_FUEL, () -> HEAVY_FUEL_FLOWING, () -> HEAVY_FUEL_BLOCK, () -> HEAVY_FUEL_BUCKET)
        );

        METHANE = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane"),
                new BasicFluid.Still(() -> METHANE, () -> METHANE_FLOWING, () -> METHANE_BLOCK, () -> METHANE_BUCKET)
        );
        METHANE_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane_flowing"),
                new BasicFluid.Flowing(() -> METHANE, () -> METHANE_FLOWING, () -> METHANE_BLOCK, () -> METHANE_BUCKET)
        );

        ResourceLocation sludgeId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "sludge");
        SLUDGE_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                sludgeId,
                new SludgeFluidBlock(
                        SLUDGE,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, sludgeId))
                )
        );

        ResourceLocation coalOilId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "coal_oil");
        COAL_OIL_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                coalOilId,
                new net.minecraft.world.level.block.LiquidBlock(
                        COAL_OIL,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, coalOilId))
                )
        );

        ResourceLocation crudeOilId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "crude_oil");
        CRUDE_OIL_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                crudeOilId,
                new net.minecraft.world.level.block.LiquidBlock(
                        CRUDE_OIL,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, crudeOilId))
                )
        );

        ResourceLocation lightFuelId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "light_fuel");
        LIGHT_FUEL_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                lightFuelId,
                new net.minecraft.world.level.block.LiquidBlock(
                        LIGHT_FUEL,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, lightFuelId))
                )
        );

        ResourceLocation heavyFuelId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "heavy_fuel");
        HEAVY_FUEL_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                heavyFuelId,
                new net.minecraft.world.level.block.LiquidBlock(
                        HEAVY_FUEL,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, heavyFuelId))
                )
        );

        ResourceLocation methaneId = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, "methane");
        METHANE_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                methaneId,
                new net.minecraft.world.level.block.LiquidBlock(
                        METHANE,
                        BlockBehaviour.Properties.of()
                                .noCollission()
                                .strength(100.0F)
                                .noLootTable()
                                .setId(ResourceKey.create(Registries.BLOCK, methaneId))
                )
        );

        SLUDGE_BUCKET = registerBucket("sludge_bucket", SLUDGE);
        COAL_OIL_BUCKET = registerBucket("coal_oil_bucket", COAL_OIL);
        CRUDE_OIL_BUCKET = registerBucket("crude_oil_bucket", CRUDE_OIL);
        LIGHT_FUEL_BUCKET = registerBucket("light_fuel_bucket", LIGHT_FUEL);
        HEAVY_FUEL_BUCKET = registerBucket("heavy_fuel_bucket", HEAVY_FUEL);
        METHANE_BUCKET = registerBucket("methane_bucket", METHANE);
    }

    private static Item registerBucket(String name, Fluid fluid) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BillTech.MOD_ID, name);
        Item.Properties props = new Item.Properties()
                .stacksTo(1)
                .craftRemainder(ModItems.BILLTECH_BUCKET)
                .setId(ResourceKey.create(Registries.ITEM, id));
        return Registry.register(BuiltInRegistries.ITEM, id, new BucketItem(fluid, props));
    }

    private static abstract class BasicFluid extends net.minecraft.world.level.material.FlowingFluid {
        private final java.util.function.Supplier<FlowingFluid> still;
        private final java.util.function.Supplier<FlowingFluid> flowing;
        private final java.util.function.Supplier<Block> block;
        private final java.util.function.Supplier<Item> bucket;

        private BasicFluid(
                java.util.function.Supplier<FlowingFluid> still,
                java.util.function.Supplier<FlowingFluid> flowing,
                java.util.function.Supplier<Block> block,
                java.util.function.Supplier<Item> bucket
        ) {
            this.still = still;
            this.flowing = flowing;
            this.block = block;
            this.bucket = bucket;
        }

        @Override
        public Fluid getFlowing() {
            return flowing.get();
        }

        @Override
        public Fluid getSource() {
            return still.get();
        }

        @Override
        public Item getBucket() {
            return bucket.get();
        }

        protected Block getBlock() {
            return block.get();
        }

        @Override
        protected net.minecraft.world.level.block.state.BlockState createLegacyBlock(FluidState state) {
            return getBlock()
                    .defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        protected boolean canConvertToSource(ServerLevel level) {
            return false;
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return 2;
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 5;
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return 2;
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            // no-op
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == still.get() || fluid == flowing.get();
        }

        public static class Still extends BasicFluid {
            public Still(
                    java.util.function.Supplier<FlowingFluid> still,
                    java.util.function.Supplier<FlowingFluid> flowing,
                    java.util.function.Supplier<Block> block,
                    java.util.function.Supplier<Item> bucket
            ) {
                super(still, flowing, block, bucket);
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }

            @Override
            public int getAmount(FluidState state) {
                return 8;
            }
        }

        public static class Flowing extends BasicFluid {
            public Flowing(
                    java.util.function.Supplier<FlowingFluid> still,
                    java.util.function.Supplier<FlowingFluid> flowing,
                    java.util.function.Supplier<Block> block,
                    java.util.function.Supplier<Item> bucket
            ) {
                super(still, flowing, block, bucket);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }
        }
    }

    private static abstract class SludgeFluid extends BasicFluid {
        private SludgeFluid(
                java.util.function.Supplier<FlowingFluid> still,
                java.util.function.Supplier<FlowingFluid> flowing,
                java.util.function.Supplier<Block> block,
                java.util.function.Supplier<Item> bucket
        ) {
            super(still, flowing, block, bucket);
        }

        public static class Still extends SludgeFluid {
            public Still() {
                super(() -> SLUDGE, () -> SLUDGE_FLOWING, () -> SLUDGE_BLOCK, () -> SLUDGE_BUCKET);
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }

            @Override
            public int getAmount(FluidState state) {
                return 8;
            }
        }

        public static class Flowing extends SludgeFluid {
            public Flowing() {
                super(() -> SLUDGE, () -> SLUDGE_FLOWING, () -> SLUDGE_BLOCK, () -> SLUDGE_BUCKET);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }
        }
    }
}
