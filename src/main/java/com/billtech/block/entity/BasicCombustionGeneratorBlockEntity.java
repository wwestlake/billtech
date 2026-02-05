package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.transport.TransportType;
import com.billtech.upgrade.UpgradeInventoryProvider;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
import com.billtech.menu.BasicCombustionGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class BasicCombustionGeneratorBlockEntity extends BlockEntity implements WorldlyContainer, UpgradeInventoryProvider, MenuProvider, SideConfigAccess {
    private static final int SLOT_FUEL = 0;
    private static final int[] SLOTS = new int[]{SLOT_FUEL};

    private final long energyCapacity;
    private final long generationPerTick;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(4, ItemStack.EMPTY);
    private int burnTime;
    private int burnTimeTotal;

    private final EnergyStorageImpl energy;
    private final EnergyStorage energyInputView;
    private final EnergyStorage energyOutputView;
    private final EnergyStorage energyBothView;
    private final UpgradeContainer upgradeContainer = new UpgradeContainer();

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

    public BasicCombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIC_COMBUSTION_GENERATOR, pos, state);
        energyCapacity = BillTechConfig.get().generator.energyCapacity;
        generationPerTick = BillTechConfig.get().generator.generationPerTick;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.NONE);
            sideConfig.set(TransportType.ITEM, dir, PortMode.NONE);
        }
        sideConfig.set(TransportType.ITEM, getFuelSide(), PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BasicCombustionGeneratorBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        if (burnTime > 0) {
            burnTime--;
            long inserted;
            try (Transaction tx = Transaction.openOuter()) {
                inserted = energy.insert(generationPerTick, tx);
                if (inserted > 0) {
                    tx.commit();
                }
            }
        }
        if (burnTime <= 0) {
            ItemStack fuel = items.get(SLOT_FUEL);
            int time = getFuelTime(fuel);
            if (time > 0 && energy.getAmount() < energy.getCapacity()) {
                burnTime = time;
                burnTimeTotal = time;
                fuel.shrink(1);
                setChanged();
            }
        }
    }

    private long getEffectiveEnergyCapacity() {
        int upgrades = Math.min(4, getUpgradeCount(UpgradeType.ENERGY));
        BillTechConfig.Upgrades cfg = BillTechConfig.get().upgrades;
        long extra = Math.round(cfg.energyCapacityPerUpgrade * upgrades * cfg.multiplier);
        return Math.max(0, energyCapacity + extra);
    }

    private long getMaxEnergyCapacity() {
        BillTechConfig.Upgrades cfg = BillTechConfig.get().upgrades;
        long extra = Math.round(cfg.energyCapacityPerUpgrade * 4 * cfg.multiplier);
        return Math.max(0, energyCapacity + extra);
    }

    private void clampEnergyToEffectiveCapacity() {
        long effective = getEffectiveEnergyCapacity();
        if (energy.amount > effective) {
            energy.setAmount(effective);
        }
    }

    private int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item == Items.COAL) {
            return 1600;
        }
        if (item == Items.CHARCOAL) {
            return 1600;
        }
        if (item == Items.BLAZE_ROD) {
            return 2400;
        }
        if (item == Items.STICK) {
            return 100;
        }
        if (item == Items.OAK_LOG || item == Items.SPRUCE_LOG || item == Items.BIRCH_LOG
                || item == Items.JUNGLE_LOG || item == Items.ACACIA_LOG || item == Items.DARK_OAK_LOG
                || item == Items.MANGROVE_LOG || item == Items.CHERRY_LOG || item == Items.CRIMSON_STEM
                || item == Items.WARPED_STEM) {
            return 300;
        }
        if (item == Items.OAK_PLANKS || item == Items.SPRUCE_PLANKS || item == Items.BIRCH_PLANKS
                || item == Items.JUNGLE_PLANKS || item == Items.ACACIA_PLANKS || item == Items.DARK_OAK_PLANKS
                || item == Items.MANGROVE_PLANKS || item == Items.CHERRY_PLANKS || item == Items.CRIMSON_PLANKS
                || item == Items.WARPED_PLANKS) {
            return 300;
        }
        return 0;
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
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putLong("Energy", energy.getAmount());
        ContainerHelper.saveAllItems(tag, items, provider);
        CompoundTag upgradesTag = new CompoundTag();
        ContainerHelper.saveAllItems(upgradesTag, upgrades, provider);
        tag.put("Upgrades", upgradesTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        burnTime = tag.getInt("BurnTime").orElse(0);
        burnTimeTotal = tag.getInt("BurnTimeTotal").orElse(0);
        long stored = tag.getLong("Energy").orElse(0L);
        energy.setAmount(stored);
        ContainerHelper.loadAllItems(tag, items, provider);
        CompoundTag upgradesTag = tag.getCompound("Upgrades").orElse(null);
        if (upgradesTag != null) {
            ContainerHelper.loadAllItems(upgradesTag, upgrades, provider);
        }
        sideConfig.load(tag, provider);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return sideConfig.allowsInsert(TransportType.ITEM, direction) ? SLOTS : new int[0];
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

    private Direction getFuelSide() {
        Direction facing = getBlockState().getValue(com.billtech.block.BasicCombustionGeneratorBlock.FACING);
        return facing.getOpposite();
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
    public Component getDisplayName() {
        return Component.translatable("container.billtech.basic_combustion_generator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new BasicCombustionGeneratorMenu(id, inventory, this);
    }

    @Override
    public Container getUpgradeContainer() {
        return upgradeContainer;
    }

    @Override
    public int getUpgradeCount(UpgradeType type) {
        int count = 0;
        for (ItemStack stack : upgrades) {
            if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private final class UpgradeContainer implements Container {
        @Override
        public int getContainerSize() {
            return upgrades.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : upgrades) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return upgrades.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = ContainerHelper.removeItem(upgrades, slot, amount);
            if (!result.isEmpty()) {
                setChanged();
            }
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(upgrades, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            upgrades.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            BasicCombustionGeneratorBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            upgrades.clear();
        }
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(com.billtech.block.BasicCombustionGeneratorBlock.FACING);
    }
}
