package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.ModBlocks;
import com.billtech.block.SteamBoilerBlock;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.menu.SteamBoilerMenu;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class SteamBoilerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, SideConfigAccess, MachineStatusAccess {
    private static final int SLOT_FUEL = 0;
    private static final int[] SLOTS = new int[]{SLOT_FUEL};

    private final long waterBuffer;
    private final long fuelBuffer;
    private final long steamBuffer;
    private final long waterPerTick;
    private final long steamPerTick;
    private final int fluidBurnTicks;
    private final long lightFuelPerCycle;
    private final long heavyFuelPerCycle;
    private final long energyCapacity;
    private final long steamForPowerPerTick;
    private final long energyFromPowerPerTick;
    private final long turbineFeedPerTick;
    private final long turbineFeedOpenThreshold;
    private final long turbineFeedCloseThreshold;

    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private int burnTime;
    private int burnTimeTotal;
    private boolean multiblockComplete;
    private boolean turbineFeedEnabled;

    private final WaterStorage waterStorage = new WaterStorage();
    private final FuelStorage fuelStorage = new FuelStorage();
    private final OutputStorage steamStorage = new OutputStorage();

    private final CombinedStorage<FluidVariant, SingleVariantStorage<FluidVariant>> allStorage =
            new CombinedStorage<>(List.of(waterStorage, fuelStorage, steamStorage));

    private final Storage<FluidVariant> inputView = new InputView();
    private final Storage<FluidVariant> outputView = new OutputView();
    private final Storage<FluidVariant> ioView = new IoView();
    private final EnergyStorageImpl energy = new EnergyStorageImpl();
    private final EnergyStorage energyInputView = new EnergyStorageView(true, false);
    private final EnergyStorage energyOutputView = new EnergyStorageView(false, true);
    private final EnergyStorage energyBothView = new EnergyStorageView(true, true);

    private final class EnergyStorageImpl extends SimpleEnergyStorage {
        private EnergyStorageImpl() {
            super(getMaxEnergyCapacity(), getMaxEnergyCapacity(), getMaxEnergyCapacity());
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }

        private void setAmount(long amount) {
            this.amount = Math.min(amount, this.capacity);
        }

        @Override
        public long getCapacity() {
            return getEffectiveEnergyCapacity();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            long effective = getEffectiveEnergyCapacity();
            if (amount >= effective) {
                return 0;
            }
            long allowed = Math.min(maxAmount, effective - amount);
            return super.insert(allowed, transaction);
        }
    }

    private final class EnergyStorageView implements EnergyStorage {
        private final boolean allowInsert;
        private final boolean allowExtract;

        private EnergyStorageView(boolean allowInsert, boolean allowExtract) {
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            return allowInsert ? energy.insert(maxAmount, transaction) : 0;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return allowExtract ? energy.extract(maxAmount, transaction) : 0;
        }

        @Override
        public long getAmount() {
            return energy.getAmount();
        }

        @Override
        public long getCapacity() {
            return energy.getCapacity();
        }

        @Override
        public boolean supportsInsertion() {
            return allowInsert;
        }

        @Override
        public boolean supportsExtraction() {
            return allowExtract;
        }
    }

    public SteamBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_BOILER, pos, state);
        BillTechConfig.SteamBoiler cfg = BillTechConfig.get().steamBoiler;
        waterBuffer = cfg.waterBuffer;
        fuelBuffer = cfg.fuelBuffer;
        steamBuffer = cfg.steamBuffer;
        waterPerTick = cfg.waterPerTick;
        steamPerTick = cfg.steamPerTick;
        fluidBurnTicks = cfg.fluidBurnTicks;
        lightFuelPerCycle = cfg.lightFuelPerCycle;
        heavyFuelPerCycle = cfg.heavyFuelPerCycle;
        energyCapacity = cfg.energyCapacity;
        steamForPowerPerTick = cfg.steamForPowerPerTick;
        energyFromPowerPerTick = cfg.energyFromPowerPerTick;
        turbineFeedPerTick = cfg.turbineFeedPerTick;
        int openPct = Math.max(0, Math.min(100, cfg.turbineFeedOpenPercent));
        int closePct = Math.max(0, Math.min(100, cfg.turbineFeedClosePercent));
        turbineFeedOpenThreshold = (steamBuffer * openPct) / 100;
        turbineFeedCloseThreshold = (steamBuffer * closePct) / 100;

        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.FLUID, dir, PortMode.NONE);
            sideConfig.set(TransportType.ITEM, dir, PortMode.NONE);
            sideConfig.set(TransportType.ENERGY, dir, PortMode.NONE);
        }
        sideConfig.set(TransportType.FLUID, Direction.UP, PortMode.INPUT);
        sideConfig.set(TransportType.FLUID, getFuelSide(), PortMode.INPUT);
        sideConfig.set(TransportType.FLUID, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ITEM, getFuelSide(), PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamBoilerBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        multiblockComplete = hasValidLine();
        tryPullInputs(level);

        if (burnTime <= 0) {
            tryRefuel();
        }

        if (burnTime <= 0 || waterStorage.getAmount() < waterPerTick || steamStorage.getAmount() + steamPerTick > steamBuffer) {
            return;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long waterTaken = waterStorage.extract(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), waterPerTick, tx);
            long steamMade = steamStorage.insert(FluidVariant.of(ModFluids.STEAM), steamPerTick, tx);
            if (waterTaken == waterPerTick && steamMade == steamPerTick) {
                burnTime--;
                tx.commit();
                setChanged();
            }
        }

        updateTurbineFeedGate();
        if (multiblockComplete && turbineFeedEnabled) {
            tryFeedTurbine(level);
        }

        tryPushSteam(level);
    }

    private void updateTurbineFeedGate() {
        long steam = steamStorage.getAmount();
        if (!turbineFeedEnabled && steam >= turbineFeedOpenThreshold) {
            turbineFeedEnabled = true;
            setChanged();
            return;
        }
        if (turbineFeedEnabled && steam <= turbineFeedCloseThreshold) {
            turbineFeedEnabled = false;
            setChanged();
        }
    }

    private void tryFeedTurbine(Level level) {
        if (steamStorage.getAmount() <= 0) {
            return;
        }
        long remaining = Math.min(turbineFeedPerTick, steamStorage.getAmount());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.is(ModBlocks.STEAM_ENGINE)) {
                continue;
            }
            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighborPos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long moved = StorageUtil.move(
                        steamStorage,
                        target,
                        variant -> variant.getFluid() == ModFluids.STEAM,
                        remaining,
                        tx
                );
                if (moved > 0) {
                    tx.commit();
                    remaining -= moved;
                    if (remaining <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void tryPullInputs(Level level) {
        if (level == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (dir == getOutputSide()) {
                continue;
            }
            BlockPos neighbor = worldPosition.relative(dir);
            Storage<FluidVariant> source = FluidStorage.SIDED.find(level, neighbor, dir.getOpposite());
            if (source == null) {
                continue;
            }
            long waterNeed = Math.max(0, waterBuffer - waterStorage.getAmount());
            long fuelNeed = Math.max(0, fuelBuffer - fuelStorage.getAmount());
            if (waterNeed <= 0 && fuelNeed <= 0) {
                return;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long moved = 0;
                if (waterNeed > 0) {
                    moved += StorageUtil.move(
                            source,
                            waterStorage,
                            variant -> variant.getFluid() == net.minecraft.world.level.material.Fluids.WATER,
                            waterNeed,
                            tx
                    );
                }
                if (fuelNeed > 0) {
                    moved += StorageUtil.move(
                            source,
                            fuelStorage,
                            variant -> variant.getFluid() == ModFluids.LIGHT_FUEL || variant.getFluid() == ModFluids.HEAVY_FUEL,
                            fuelNeed,
                            tx
                    );
                }
                if (moved > 0) {
                    tx.commit();
                }
            }
        }
    }

    private void convertSteamToPower() {
        if (steamStorage.getAmount() <= 0 || energy.getAmount() >= energy.getCapacity()) {
            return;
        }
        long reserve = Math.max(1L, steamPerTick);
        long usableSteam = Math.max(0L, steamStorage.getAmount() - reserve);
        long availableSteam = Math.min(usableSteam, steamForPowerPerTick);
        if (availableSteam <= 0) {
            return;
        }
        long potentialEnergy = (availableSteam * energyFromPowerPerTick) / Math.max(1L, steamForPowerPerTick);
        if (potentialEnergy <= 0) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long energyInserted = energy.insert(potentialEnergy, tx);
            if (energyInserted <= 0) {
                return;
            }
            long steamNeeded = (energyInserted * steamForPowerPerTick + energyFromPowerPerTick - 1) / Math.max(1L, energyFromPowerPerTick);
            steamNeeded = Math.min(steamNeeded, availableSteam);
            long steamTaken = steamStorage.extract(FluidVariant.of(ModFluids.STEAM), steamNeeded, tx);
            if (steamTaken == steamNeeded) {
                tx.commit();
            }
        }
    }

    private void tryPushSteam(Level level) {
        if (steamStorage.getAmount() <= 0) {
            return;
        }
        Direction out = getOutputSide();
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, worldPosition.relative(out), out.getOpposite());
        if (target == null) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = StorageUtil.move(steamStorage, target, variant -> true, steamStorage.getAmount(), tx);
            if (moved > 0) {
                tx.commit();
            }
        }
    }

    private void tryRefuel() {
        if (tryRefuelFromFluid()) {
            return;
        }
        ItemStack fuel = items.get(SLOT_FUEL);
        int itemFuel = getFuelTime(fuel);
        if (itemFuel > 0) {
            burnTime = itemFuel;
            burnTimeTotal = itemFuel;
            fuel.shrink(1);
            setChanged();
        }
    }

    private boolean tryRefuelFromFluid() {
        FluidVariant light = FluidVariant.of(ModFluids.LIGHT_FUEL);
        FluidVariant heavy = FluidVariant.of(ModFluids.HEAVY_FUEL);

        try (Transaction tx = Transaction.openOuter()) {
            long heavyTaken = fuelStorage.extract(heavy, heavyFuelPerCycle, tx);
            if (heavyTaken == heavyFuelPerCycle) {
                burnTime = fluidBurnTicks * 2;
                burnTimeTotal = burnTime;
                tx.commit();
                setChanged();
                return true;
            }
        }

        try (Transaction tx = Transaction.openOuter()) {
            long lightTaken = fuelStorage.extract(light, lightFuelPerCycle, tx);
            if (lightTaken == lightFuelPerCycle) {
                burnTime = fluidBurnTicks;
                burnTimeTotal = burnTime;
                tx.commit();
                setChanged();
                return true;
            }
        }
        return false;
    }

    private int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item == Items.COAL || item == Items.CHARCOAL) {
            return 1600;
        }
        if (item == Items.BLAZE_ROD) {
            return 2400;
        }
        if (item == Items.COAL_BLOCK) {
            return 16000;
        }
        return 0;
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(SteamBoilerBlock.FACING);
        return facing.getCounterClockWise();
    }

    private Direction getFuelSide() {
        return getBlockState().getValue(SteamBoilerBlock.FACING).getOpposite();
    }

    private MultiblockLine findLine() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos firstPos = worldPosition.relative(dir);
            BlockPos secondPos = firstPos.relative(dir);
            if (level == null) {
                continue;
            }
            boolean firstEngine = level.getBlockState(firstPos).is(ModBlocks.STEAM_ENGINE);
            boolean firstGenerator = level.getBlockState(firstPos).is(ModBlocks.STEAM_GENERATOR);
            boolean secondEngine = level.getBlockState(secondPos).is(ModBlocks.STEAM_ENGINE);
            boolean secondGenerator = level.getBlockState(secondPos).is(ModBlocks.STEAM_GENERATOR);
            if (firstEngine && secondGenerator) {
                return new MultiblockLine(firstPos, secondPos);
            }
            if (firstGenerator && secondEngine) {
                return new MultiblockLine(secondPos, firstPos);
            }
        }
        return null;
    }

    private boolean hasValidLine() {
        return findLine() != null;
    }

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side == null) {
            return ioView;
        }
        if (side == getOutputSide()) {
            return outputView;
        }
        return inputView;
    }

    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        PortMode mode = sideConfig.get(TransportType.ENERGY, side);
        return switch (mode) {
            case INPUT -> energyInputView;
            case OUTPUT -> energyOutputView;
            case BOTH -> energyBothView;
            case NONE -> EnergyStorage.EMPTY;
        };
    }

    public long getWaterAmount() {
        return waterStorage.getAmount();
    }

    public long getFuelAmount() {
        return fuelStorage.getAmount();
    }

    public long getSteamAmount() {
        return steamStorage.getAmount();
    }

    public long getWaterBuffer() {
        return waterBuffer;
    }

    public long getFuelBuffer() {
        return fuelBuffer;
    }

    public long getSteamBuffer() {
        return steamBuffer;
    }

    public long getSteamPerTick() {
        return steamPerTick;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getBurnTimeTotal() {
        return burnTimeTotal;
    }

    public boolean isTurbineFeedEnabled() {
        return turbineFeedEnabled;
    }

    public boolean isMultiblockComplete() {
        return multiblockComplete;
    }

    public long getEnergyAmount() {
        return energy.getAmount();
    }

    public long getEnergyCapacityLong() {
        return energy.getCapacity();
    }

    public long getEngineSteamAmount() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.enginePos());
        if (be instanceof SteamEngineBlockEntity engine) {
            return engine.getInputAmount();
        }
        return 0;
    }

    public long getEngineSteamCapacity() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.enginePos());
        if (be instanceof SteamEngineBlockEntity engine) {
            return engine.getInputBuffer();
        }
        return 0;
    }

    public long getGeneratorSteamAmount() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.generatorPos());
        if (be instanceof SteamGeneratorBlockEntity generator) {
            return generator.getInputAmount();
        }
        return 0;
    }

    public long getGeneratorSteamCapacity() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.generatorPos());
        if (be instanceof SteamGeneratorBlockEntity generator) {
            return generator.getInputBuffer();
        }
        return 0;
    }

    public long getGeneratorEnergyAmount() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.generatorPos());
        if (be instanceof SteamGeneratorBlockEntity generator) {
            return generator.getEnergyAmount();
        }
        return 0;
    }

    public long getGeneratorEnergyCapacity() {
        MultiblockLine line = findLine();
        if (line == null || level == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(line.generatorPos());
        if (be instanceof SteamGeneratorBlockEntity generator) {
            return generator.getEnergyCapacityLong();
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putLong("Energy", energy.getAmount());
        ContainerHelper.saveAllItems(tag, items, provider);

        CompoundTag waterTag = new CompoundTag();
        SingleVariantStorage.writeNbt(waterStorage, FluidVariant.CODEC, waterTag, provider);
        tag.put("Water", waterTag);

        CompoundTag fuelTag = new CompoundTag();
        SingleVariantStorage.writeNbt(fuelStorage, FluidVariant.CODEC, fuelTag, provider);
        tag.put("Fuel", fuelTag);

        CompoundTag steamTag = new CompoundTag();
        SingleVariantStorage.writeNbt(steamStorage, FluidVariant.CODEC, steamTag, provider);
        tag.put("Steam", steamTag);

        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        burnTime = tag.getInt("BurnTime").orElse(0);
        burnTimeTotal = tag.getInt("BurnTimeTotal").orElse(0);
        long storedEnergy = tag.getLong("Energy").orElse(0L);
        energy.setAmount(storedEnergy);
        ContainerHelper.loadAllItems(tag, items, provider);

        CompoundTag waterTag = tag.getCompound("Water").orElse(null);
        if (waterTag != null) {
            SingleVariantStorage.readNbt(waterStorage, FluidVariant.CODEC, FluidVariant::blank, waterTag, provider);
        }

        CompoundTag fuelTag = tag.getCompound("Fuel").orElse(null);
        if (fuelTag != null) {
            SingleVariantStorage.readNbt(fuelStorage, FluidVariant.CODEC, FluidVariant::blank, fuelTag, provider);
        }

        CompoundTag steamTag = tag.getCompound("Steam").orElse(null);
        if (steamTag != null) {
            SingleVariantStorage.readNbt(steamStorage, FluidVariant.CODEC, FluidVariant::blank, steamTag, provider);
        }

        sideConfig.load(tag, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.steam_boiler");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new SteamBoilerMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(SteamBoilerBlock.FACING);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return sideConfig.allowsInsert(TransportType.ITEM, side) ? SLOTS : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot != SLOT_FUEL) {
            return false;
        }
        if (dir != null && !sideConfig.allowsInsert(TransportType.ITEM, dir)) {
            return false;
        }
        return getFuelTime(stack) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int getEnergyStored() {
        return clampLong(energy.getAmount());
    }

    @Override
    public int getEnergyCapacity() {
        return clampLong(getEffectiveEnergyCapacity());
    }

    @Override
    public int getFluidInStored() {
        return clampLong(waterStorage.getAmount() + fuelStorage.getAmount());
    }

    @Override
    public int getFluidInCapacity() {
        return clampLong(waterBuffer + fuelBuffer);
    }

    @Override
    public int getFluidOutStored() {
        return clampLong(steamStorage.getAmount());
    }

    @Override
    public int getFluidOutCapacity() {
        return clampLong(steamBuffer);
    }

    private long getEffectiveEnergyCapacity() {
        return energyCapacity;
    }

    private long getMaxEnergyCapacity() {
        return energyCapacity;
    }

    private void clampEnergyToEffectiveCapacity() {
        long effective = getEffectiveEnergyCapacity();
        if (energy.amount > effective) {
            energy.setAmount(effective);
        }
    }

    private final class WaterStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return waterBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return variant != null && !variant.isBlank() && variant.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                    && (this.variant == null || this.variant.isBlank() || this.variant.equals(variant));
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return variant != null && !variant.isBlank() && this.variant != null && this.variant.equals(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }

    private final class FuelStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return fuelBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            boolean isFuel = variant.getFluid() == ModFluids.LIGHT_FUEL || variant.getFluid() == ModFluids.HEAVY_FUEL;
            return isFuel && (this.variant == null || this.variant.isBlank() || this.variant.equals(variant));
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return variant != null && !variant.isBlank() && this.variant != null && this.variant.equals(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }

    private final class OutputStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return steamBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            boolean isSteam = variant.getFluid() == ModFluids.STEAM;
            return isSteam && (this.variant == null || this.variant.isBlank() || this.variant.equals(variant));
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return variant != null && !variant.isBlank() && this.variant != null && this.variant.equals(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }

    private final class InputView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.getFluid() == net.minecraft.world.level.material.Fluids.WATER) {
                return waterStorage.insert(resource, maxAmount, transaction);
            }
            if (resource.getFluid() == ModFluids.LIGHT_FUEL || resource.getFluid() == ModFluids.HEAVY_FUEL) {
                return fuelStorage.insert(resource, maxAmount, transaction);
            }
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return allStorage.iterator();
        }
    }

    private final class OutputView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return steamStorage.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return steamStorage.iterator();
        }
    }

    private final class IoView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return inputView.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return outputView.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return allStorage.iterator();
        }
    }

    private record MultiblockLine(BlockPos enginePos, BlockPos generatorPos) {}
}
