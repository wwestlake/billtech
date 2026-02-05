package com.billtech.block.entity;

import com.billtech.block.DistillerBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class DistillerBlockEntity extends BlockEntity implements MenuProvider, SideConfigAccess {
    private final long energyCapacity;
    private final long energyPerTick;
    private final int ticksPerBatch;
    private final long inputPerBatch;
    private final long outputLight;
    private final long outputHeavy;
    private final long outputSludge;
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

    private final OutputStorage lightStorage = new OutputStorage(ModFluids.LIGHT_FUEL);
    private final OutputStorage heavyStorage = new OutputStorage(ModFluids.HEAVY_FUEL);
    private final OutputStorage sludgeStorage = new OutputStorage(ModFluids.SLUDGE);
    private final CombinedStorage<FluidVariant, OutputStorage> outputCombined =
            new CombinedStorage<>(List.of(lightStorage, heavyStorage, sludgeStorage));
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

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTILLER, pos, state);
        BillTechConfig.Distiller cfg = BillTechConfig.get().distiller;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        ticksPerBatch = cfg.ticksPerBatch;
        inputPerBatch = cfg.inputPerBatch;
        outputLight = cfg.outputLight;
        outputHeavy = cfg.outputHeavy;
        outputSludge = cfg.outputSludge;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, DistillerBlockEntity be) {
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
        if (lightStorage.getAmount() <= 0 && heavyStorage.getAmount() <= 0 && sludgeStorage.getAmount() <= 0) {
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
        if (heavyStorage.getAmount() + outputHeavy > outputBuffer) {
            return false;
        }
        if (sludgeStorage.getAmount() + outputSludge > outputBuffer) {
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
            long lightInserted = lightStorage.insert(FluidVariant.of(ModFluids.LIGHT_FUEL), outputLight, tx);
            long heavyInserted = heavyStorage.insert(FluidVariant.of(ModFluids.HEAVY_FUEL), outputHeavy, tx);
            long sludgeInserted = sludgeStorage.insert(FluidVariant.of(ModFluids.SLUDGE), outputSludge, tx);
            if (lightInserted == outputLight && heavyInserted == outputHeavy && sludgeInserted == outputSludge) {
                tx.commit();
                setChanged();
            }
        }
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(DistillerBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(DistillerBlock.FACING);
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
        CompoundTag heavyTag = new CompoundTag();
        SingleVariantStorage.writeNbt(heavyStorage, FluidVariant.CODEC, heavyTag, provider);
        tag.put("Heavy", heavyTag);
        CompoundTag sludgeTag = new CompoundTag();
        SingleVariantStorage.writeNbt(sludgeStorage, FluidVariant.CODEC, sludgeTag, provider);
        tag.put("Sludge", sludgeTag);
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
        CompoundTag heavyTag = tag.getCompound("Heavy").orElse(null);
        if (heavyTag != null) {
            SingleVariantStorage.readNbt(heavyStorage, FluidVariant.CODEC, FluidVariant::blank, heavyTag, provider);
        }
        CompoundTag sludgeTag = tag.getCompound("Sludge").orElse(null);
        if (sludgeTag != null) {
            SingleVariantStorage.readNbt(sludgeStorage, FluidVariant.CODEC, FluidVariant::blank, sludgeTag, provider);
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
        return Component.translatable("container.billtech.distiller");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.DistillerMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(DistillerBlock.FACING);
    }
}
