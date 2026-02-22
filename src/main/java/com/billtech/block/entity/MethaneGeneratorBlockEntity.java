package com.billtech.block.entity;

import com.billtech.block.MethaneGeneratorBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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

public class MethaneGeneratorBlockEntity extends BlockEntity implements MenuProvider, SideConfigAccess, MachineStatusAccess, RemoteControllable {
    private final long energyCapacity;
    private final long energyPerTick;
    private final long methanePerTick;
    private final long inputBuffer;
    private boolean remoteEnabled = true;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);

    private final InputStorage inputStorage = new InputStorage();
    private final Storage<FluidVariant> inputView = new InputStorageView();

    private final EnergyStorageImpl energy;
    private final EnergyStorage energyInputView;
    private final EnergyStorage energyOutputView;
    private final EnergyStorage energyBothView;

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
            FluidVariant target = FluidVariant.of(ModFluids.METHANE);
            if (!variant.equals(target)) {
                return false;
            }
            return this.variant == null || this.variant.isBlank() || this.variant.equals(variant);
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
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

    public MethaneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METHANE_GENERATOR, pos, state);
        BillTechConfig.MethaneGenerator cfg = BillTechConfig.get().methaneGenerator;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        methanePerTick = cfg.methanePerTick;
        inputBuffer = cfg.inputBuffer;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        sideConfig.set(TransportType.FLUID, getInputSide(), PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MethaneGeneratorBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        if (!remoteEnabled) {
            return;
        }
        if (energy.getAmount() >= energy.getCapacity()) {
            return;
        }
        FluidVariant methane = FluidVariant.of(ModFluids.METHANE);
        if (inputStorage.getAmount() < methanePerTick) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = inputStorage.extract(methane, methanePerTick, tx);
            if (extracted == methanePerTick) {
                long inserted = energy.insert(energyPerTick, tx);
                if (inserted > 0) {
                    tx.commit();
                }
            }
        }
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

    public long getEnergyAmount() {
        return energy.getAmount();
    }

    public long getEnergyCapacityLong() {
        return energy.getCapacity();
    }

    public long getInputAmount() {
        return inputStorage.getAmount();
    }

    public long getInputBuffer() {
        return inputBuffer;
    }

    public long getMethanePerTick() {
        return methanePerTick;
    }

    public long getEnergyPerTick() {
        return energyPerTick;
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(MethaneGeneratorBlock.FACING);
        return facing.getClockWise();
    }

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side == null) {
            return inputView;
        }
        if (sideConfig.allowsInsert(TransportType.FLUID, side)) {
            return inputView;
        }
        return Storage.empty();
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energy.getAmount());
        tag.putBoolean("RemoteEnabled", remoteEnabled);
        CompoundTag fluidTag = new CompoundTag();
        SingleVariantStorage.writeNbt(inputStorage, FluidVariant.CODEC, fluidTag, provider);
        tag.put("Input", fluidTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        long stored = tag.getLong("Energy").orElse(0L);
        energy.setAmount(stored);
        remoteEnabled = tag.getBoolean("RemoteEnabled").orElse(true);
        CompoundTag fluidTag = tag.getCompound("Input").orElse(null);
        if (fluidTag != null) {
            SingleVariantStorage.readNbt(inputStorage, FluidVariant.CODEC, FluidVariant::blank, fluidTag, provider);
        }
        sideConfig.load(tag, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.methane_generator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.MethaneGeneratorMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(MethaneGeneratorBlock.FACING);
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
        return 0;
    }

    @Override
    public int getFluidOutCapacity() {
        return 0;
    }

    @Override
    public MachineRuntimeState getRuntimeState() {
        if (!remoteEnabled) {
            return MachineRuntimeState.DISABLED;
        }
        if (energy.getAmount() >= energy.getCapacity()) {
            return MachineRuntimeState.OUTPUT_FULL;
        }
        if (inputStorage.getAmount() < methanePerTick) {
            return MachineRuntimeState.NO_WORK;
        }
        return MachineRuntimeState.RUNNING;
    }

    @Override
    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }

    @Override
    public void setRemoteEnabled(boolean enabled) {
        if (remoteEnabled == enabled) {
            return;
        }
        remoteEnabled = enabled;
        setChanged();
    }
}
