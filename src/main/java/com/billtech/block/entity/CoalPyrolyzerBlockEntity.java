package com.billtech.block.entity;

import com.billtech.block.CoalPyrolyzerBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class CoalPyrolyzerBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer, MenuProvider, SideConfigAccess, MachineStatusAccess {
    private static final int SLOT_INPUT = 0;
    private static final int[] SLOTS_INPUT = new int[]{SLOT_INPUT};

    private final long energyCapacity;
    private final long energyPerTick;
    private final int ticksPerItem;
    private final long outputPerItem;
    private final long outputBuffer;

    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private int cookTime;

    private final EnergyStorageImpl energy;
    private final EnergyStorage energyInputView;
    private final EnergyStorage energyOutputView;
    private final EnergyStorage energyBothView;

    private final OutputStorage outputStorage = new OutputStorage();
    private final Storage<FluidVariant> outputView = new OutputStorageView();

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

    private final class OutputStorage extends SingleVariantStorage<FluidVariant> {
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

    private final class OutputStorageView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return outputStorage.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return outputStorage.iterator();
        }
    }

    public CoalPyrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_PYROLYZER, pos, state);
        BillTechConfig.CoalPyrolyzer cfg = BillTechConfig.get().coalPyrolyzer;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        ticksPerItem = cfg.ticksPerItem;
        outputPerItem = cfg.outputPerItem;
        outputBuffer = cfg.outputBuffer;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.INPUT);
        }
        sideConfig.set(TransportType.ITEM, getInputSide(), PortMode.INPUT);
        sideConfig.set(TransportType.FLUID, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoalPyrolyzerBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        tryPushOutput(level);
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || !isValidInput(input)) {
            cookTime = 0;
            return;
        }
        if (!canAcceptOutput()) {
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
        if (cookTime >= ticksPerItem) {
            finishRecipe();
            cookTime = 0;
        }
    }

    private void tryPushOutput(Level level) {
        if (level == null) {
            return;
        }
        if (outputStorage.getAmount() <= 0) {
            return;
        }
        Direction out = getOutputSide();
        BlockPos neighbor = worldPosition.relative(out);
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbor, out.getOpposite());
        if (target == null) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = StorageUtil.move(outputStorage, target, variant -> true, outputStorage.getAmount(), tx);
            if (moved > 0) {
                tx.commit();
            }
        }
    }

    private boolean isValidInput(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    private boolean canAcceptOutput() {
        long current = outputStorage.getAmount();
        return current + outputPerItem <= outputBuffer;
    }

    private void finishRecipe() {
        FluidVariant crude = FluidVariant.of(ModFluids.CRUDE_OIL);
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = outputStorage.insert(crude, outputPerItem, tx);
            if (inserted == outputPerItem) {
                tx.commit();
                items.get(SLOT_INPUT).shrink(1);
                setChanged();
            }
        }
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(CoalPyrolyzerBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getBackSide() {
        return getBlockState().getValue(CoalPyrolyzerBlock.FACING).getOpposite();
    }

    private boolean isInputSide(@Nullable Direction side) {
        if (side == null) {
            return true;
        }
        return side == getInputSide() || side == getBackSide();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(CoalPyrolyzerBlock.FACING);
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
            return outputView;
        }
        if (sideConfig.allowsExtract(TransportType.FLUID, side)) {
            return outputView;
        }
        return Storage.empty();
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return sideConfig.allowsInsert(TransportType.ITEM, direction) ? SLOTS_INPUT : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == SLOT_INPUT
                && (dir == null || sideConfig.allowsInsert(TransportType.ITEM, dir))
                && isValidInput(stack);
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
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("CookTime", cookTime);
        tag.putLong("Energy", energy.getAmount());
        ContainerHelper.saveAllItems(tag, items, provider);
        CompoundTag fluidTag = new CompoundTag();
        SingleVariantStorage.writeNbt(outputStorage, FluidVariant.CODEC, fluidTag, provider);
        tag.put("Output", fluidTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cookTime = tag.getInt("CookTime").orElse(0);
        long stored = tag.getLong("Energy").orElse(0L);
        energy.setAmount(stored);
        ContainerHelper.loadAllItems(tag, items, provider);
        CompoundTag fluidTag = tag.getCompound("Output").orElse(null);
        if (fluidTag != null) {
            SingleVariantStorage.readNbt(outputStorage, FluidVariant.CODEC, FluidVariant::blank, fluidTag, provider);
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
        return Component.translatable("container.billtech.coal_pyrolyzer");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.CoalPyrolyzerMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(CoalPyrolyzerBlock.FACING);
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
        return 0;
    }

    @Override
    public int getFluidInCapacity() {
        return 0;
    }

    @Override
    public int getFluidOutStored() {
        return clampLong(outputStorage.getAmount());
    }

    @Override
    public int getFluidOutCapacity() {
        return clampLong(outputBuffer);
    }
}
