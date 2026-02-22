package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.PaperPressBlock;
import com.billtech.config.BillTechConfig;
import com.billtech.item.ModItems;
import com.billtech.transport.TransportType;
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
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class PaperPressBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, SideConfigAccess, MachineStatusAccess, RemoteControllable {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int[] SLOTS_INPUT = new int[]{SLOT_INPUT};
    private static final int[] SLOTS_OUTPUT = new int[]{SLOT_OUTPUT};

    private final long energyCapacity;
    private final long energyPerTick;
    private final int ticksPerItem;

    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int cookTime;
    private boolean remoteEnabled = true;

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

    public PaperPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAPER_PRESS, pos, state);
        BillTechConfig.PaperPress cfg = BillTechConfig.get().paperPress;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        ticksPerItem = cfg.ticksPerItem;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.INPUT);
        }
        sideConfig.set(TransportType.ITEM, getInputSide(), PortMode.INPUT);
        sideConfig.set(TransportType.ITEM, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PaperPressBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        if (!remoteEnabled) {
            cookTime = 0;
            return;
        }
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

    private boolean isValidInput(ItemStack stack) {
        return stack.is(Items.PAPER);
    }

    private boolean canAcceptOutput() {
        ItemStack output = items.get(SLOT_OUTPUT);
        ItemStack result = new ItemStack(ModItems.PLASTIC_SHEET);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishRecipe() {
        ItemStack output = items.get(SLOT_OUTPUT);
        ItemStack result = new ItemStack(ModItems.PLASTIC_SHEET);
        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        items.get(SLOT_INPUT).shrink(1);
        setChanged();
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(PaperPressBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getBackSide() {
        return getBlockState().getValue(PaperPressBlock.FACING).getOpposite();
    }

    private boolean isInputSide(@Nullable Direction side) {
        if (side == null) {
            return true;
        }
        return side == getInputSide() || side == getBackSide();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(PaperPressBlock.FACING);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("CookTime", cookTime);
        tag.putBoolean("RemoteEnabled", remoteEnabled);
        tag.putLong("Energy", energy.getAmount());
        ContainerHelper.saveAllItems(tag, items, provider);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cookTime = tag.getInt("CookTime").orElse(0);
        remoteEnabled = tag.getBoolean("RemoteEnabled").orElse(true);
        long stored = tag.getLong("Energy").orElse(0L);
        energy.setAmount(stored);
        ContainerHelper.loadAllItems(tag, items, provider);
        sideConfig.load(tag, provider);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (sideConfig.allowsInsert(TransportType.ITEM, direction)) {
            return SLOTS_INPUT;
        }
        if (sideConfig.allowsExtract(TransportType.ITEM, direction)) {
            return SLOTS_OUTPUT;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot != SLOT_INPUT) {
            return false;
        }
        return (dir == null || sideConfig.allowsInsert(TransportType.ITEM, dir)) && isValidInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT && sideConfig.allowsExtract(TransportType.ITEM, dir);
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
        return Component.translatable("container.billtech.paper_press");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.PaperPressMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(PaperPressBlock.FACING);
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
        if (cookTime > 0) {
            return MachineRuntimeState.RUNNING;
        }
        if (energy.getAmount() < energyPerTick) {
            return MachineRuntimeState.NO_POWER;
        }
        return items.get(SLOT_INPUT).isEmpty() ? MachineRuntimeState.NO_WORK : MachineRuntimeState.IDLE;
    }

    @Override
    public int getProcessProgress() {
        return cookTime;
    }

    @Override
    public int getProcessMax() {
        return ticksPerItem;
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
        if (!enabled) {
            cookTime = 0;
        }
        setChanged();
    }
}
