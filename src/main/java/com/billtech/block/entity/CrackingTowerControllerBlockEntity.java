package com.billtech.block.entity;

import com.billtech.block.CrackingTowerControllerBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class CrackingTowerControllerBlockEntity extends BlockEntity implements MenuProvider, SideConfigAccess, MachineStatusAccess {
    private final long energyCapacity;
    private final long energyPerTick;
    private final int ticksPerBatch;
    private final long inputPerBatch;
    private final long outputLight;
    private final long outputMedium;
    private final long outputHeavy;
    private final long outputResidue;
    private final long inputBuffer;
    private final long outputBuffer;

    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private int cookTime;

    private final EnergyStorageImpl energy;
    private final EnergyStorage energyInputView;
    private final EnergyStorage energyOutputView;
    private final EnergyStorage energyBothView;

    private final InputStorage inputStorage = new InputStorage();
    private final Storage<FluidVariant> inputView = new InputStorageView();

    private final OutputStorage lightStorage = new OutputStorage(ModFluids.LIGHT_FRACTION);
    private final OutputStorage mediumStorage = new OutputStorage(ModFluids.MEDIUM_FRACTION);
    private final OutputStorage heavyStorage = new OutputStorage(ModFluids.HEAVY_FRACTION);
    private final OutputStorage residueStorage = new OutputStorage(ModFluids.RESIDUE);
    private final CombinedStorage<FluidVariant, OutputStorage> outputCombined =
            new CombinedStorage<>(List.of(lightStorage, mediumStorage, heavyStorage, residueStorage));
    private final Storage<FluidVariant> outputView = new OutputStorageView();
    private final Storage<FluidVariant> ioView = new IoStorageView();

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

    private final class InputStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return inputBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            FluidVariant target = FluidVariant.of(ModFluids.CRUDE_OIL);
            if (!variant.equals(target)) {
                return false;
            }
            return this.variant == null || this.variant.isBlank() || this.variant.equals(variant);
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

    private final class InputStorageView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return inputStorage.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return inputStorage.iterator();
        }
    }

    private final class OutputStorage extends SingleVariantStorage<FluidVariant> {
        private final FluidVariant target;

        private OutputStorage(net.minecraft.world.level.material.Fluid fluid) {
            this.target = FluidVariant.of(fluid);
        }

        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return outputBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            if (!variant.equals(target)) {
                return false;
            }
            return this.variant == null || this.variant.isBlank() || this.variant.equals(variant);
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

    private final class OutputStorageView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return outputCombined.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return outputCombined.iterator();
        }
    }

    private final class IoStorageView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return inputStorage.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return outputCombined.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return outputCombined.iterator();
        }
    }

    public CrackingTowerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRACKING_TOWER_CONTROLLER, pos, state);
        BillTechConfig.CrackingTower cfg = BillTechConfig.get().crackingTower;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        ticksPerBatch = cfg.ticksPerBatch;
        inputPerBatch = cfg.inputPerBatch;
        outputLight = cfg.outputLight;
        outputMedium = cfg.outputMedium;
        outputHeavy = cfg.outputHeavy;
        outputResidue = cfg.outputResidue;
        inputBuffer = cfg.inputBuffer;
        outputBuffer = cfg.outputBuffer;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.INPUT);
        }
        sideConfig.set(TransportType.FLUID, getInputSide(), PortMode.INPUT);
        sideConfig.set(TransportType.FLUID, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrackingTowerControllerBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        tryPushOutputs(level);
        if (!canProcess()) {
            cookTime = 0;
            return;
        }
        if (energy.getAmount() < energyPerTick) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = energy.extract(energyPerTick, tx);
            if (extracted == energyPerTick) {
                tx.commit();
                cookTime++;
            }
        }
        if (cookTime >= ticksPerBatch) {
            finishRecipe();
            cookTime = 0;
        }
    }

    private void tryPushOutputs(Level level) {
        if (level == null) {
            return;
        }
        if (lightStorage.getAmount() <= 0 && mediumStorage.getAmount() <= 0 && heavyStorage.getAmount() <= 0 && residueStorage.getAmount() <= 0) {
            return;
        }
        Direction out = getOutputSide();
        BlockPos neighbor = worldPosition.relative(out);
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbor, out.getOpposite());
        if (target == null) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = StorageUtil.move(outputCombined, target, variant -> true, outputBuffer, tx);
            if (moved > 0) {
                tx.commit();
            }
        }
    }

    private boolean canProcess() {
        if (inputStorage.getAmount() < inputPerBatch) {
            return false;
        }
        if (lightStorage.getAmount() + outputLight > outputBuffer) {
            return false;
        }
        if (mediumStorage.getAmount() + outputMedium > outputBuffer) {
            return false;
        }
        if (heavyStorage.getAmount() + outputHeavy > outputBuffer) {
            return false;
        }
        if (residueStorage.getAmount() + outputResidue > outputBuffer) {
            return false;
        }
        return true;
    }

    private void finishRecipe() {
        FluidVariant crude = FluidVariant.of(ModFluids.CRUDE_OIL);
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = inputStorage.extract(crude, inputPerBatch, tx);
            if (extracted != inputPerBatch) {
                return;
            }
            long lightInserted = lightStorage.insert(FluidVariant.of(ModFluids.LIGHT_FRACTION), outputLight, tx);
            long mediumInserted = mediumStorage.insert(FluidVariant.of(ModFluids.MEDIUM_FRACTION), outputMedium, tx);
            long heavyInserted = heavyStorage.insert(FluidVariant.of(ModFluids.HEAVY_FRACTION), outputHeavy, tx);
            long residueInserted = residueStorage.insert(FluidVariant.of(ModFluids.RESIDUE), outputResidue, tx);
            if (lightInserted == outputLight && mediumInserted == outputMedium && heavyInserted == outputHeavy && residueInserted == outputResidue) {
                tx.commit();
                setChanged();
            }
        }
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(CrackingTowerControllerBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(CrackingTowerControllerBlock.FACING);
        return facing.getCounterClockWise();
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

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side == null) {
            return ioView;
        }
        if (sideConfig.allowsInsert(TransportType.FLUID, side) && sideConfig.allowsExtract(TransportType.FLUID, side)) {
            return ioView;
        }
        if (sideConfig.allowsInsert(TransportType.FLUID, side)) {
            return inputView;
        }
        if (sideConfig.allowsExtract(TransportType.FLUID, side)) {
            return outputView;
        }
        return Storage.empty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("CookTime", cookTime);
        tag.putLong("Energy", energy.getAmount());
        CompoundTag inputTag = new CompoundTag();
        SingleVariantStorage.writeNbt(inputStorage, FluidVariant.CODEC, inputTag, provider);
        tag.put("Input", inputTag);
        CompoundTag lightTag = new CompoundTag();
        SingleVariantStorage.writeNbt(lightStorage, FluidVariant.CODEC, lightTag, provider);
        tag.put("Light", lightTag);
        CompoundTag mediumTag = new CompoundTag();
        SingleVariantStorage.writeNbt(mediumStorage, FluidVariant.CODEC, mediumTag, provider);
        tag.put("Medium", mediumTag);
        CompoundTag heavyTag = new CompoundTag();
        SingleVariantStorage.writeNbt(heavyStorage, FluidVariant.CODEC, heavyTag, provider);
        tag.put("Heavy", heavyTag);
        CompoundTag residueTag = new CompoundTag();
        SingleVariantStorage.writeNbt(residueStorage, FluidVariant.CODEC, residueTag, provider);
        tag.put("Residue", residueTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cookTime = tag.getInt("CookTime").orElse(0);
        long stored = tag.getLong("Energy").orElse(0L);
        energy.setAmount(stored);
        CompoundTag inputTag = tag.getCompound("Input").orElse(null);
        if (inputTag != null) {
            SingleVariantStorage.readNbt(inputStorage, FluidVariant.CODEC, FluidVariant::blank, inputTag, provider);
        }
        CompoundTag lightTag = tag.getCompound("Light").orElse(null);
        if (lightTag != null) {
            SingleVariantStorage.readNbt(lightStorage, FluidVariant.CODEC, FluidVariant::blank, lightTag, provider);
        }
        CompoundTag mediumTag = tag.getCompound("Medium").orElse(null);
        if (mediumTag != null) {
            SingleVariantStorage.readNbt(mediumStorage, FluidVariant.CODEC, FluidVariant::blank, mediumTag, provider);
        }
        CompoundTag heavyTag = tag.getCompound("Heavy").orElse(null);
        if (heavyTag != null) {
            SingleVariantStorage.readNbt(heavyStorage, FluidVariant.CODEC, FluidVariant::blank, heavyTag, provider);
        }
        CompoundTag residueTag = tag.getCompound("Residue").orElse(null);
        if (residueTag != null) {
            SingleVariantStorage.readNbt(residueStorage, FluidVariant.CODEC, FluidVariant::blank, residueTag, provider);
        }
        sideConfig.load(tag, provider);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.cracking_tower");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.CrackingTowerMenu(id, inventory, this);
    }


    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(CrackingTowerControllerBlock.FACING);
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
        return clampLong(inputStorage.getAmount());
    }

    @Override
    public int getFluidInCapacity() {
        return clampLong(inputBuffer);
    }

    @Override
    public int getFluidOutStored() {
        long total = lightStorage.getAmount() + mediumStorage.getAmount() + heavyStorage.getAmount() + residueStorage.getAmount();
        return clampLong(total);
    }

    @Override
    public int getFluidOutCapacity() {
        return clampLong(outputBuffer);
    }
}
