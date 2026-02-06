package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.SeparatorBlock;
import com.billtech.config.BillTechConfig;
import com.billtech.item.ModItems;
import com.billtech.menu.SeparatorMenu;
import com.billtech.transport.TransportType;
import com.billtech.upgrade.UpgradeInventoryProvider;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
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

public class SeparatorBlockEntity extends BlockEntity implements WorldlyContainer, UpgradeInventoryProvider, MenuProvider, SideConfigAccess {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_BYPRODUCT = 2;
    private static final int[] SLOTS_INPUT = new int[]{SLOT_INPUT};
    private static final int[] SLOTS_OUTPUT = new int[]{SLOT_OUTPUT, SLOT_BYPRODUCT};

    private final long energyCapacity;
    private final long energyPerTick;
    private final int ticksPerItem;
    private final int outputPerItem;
    private final double slagChance;

    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(4, ItemStack.EMPTY);
    private int cookTime;
    private int cookTimeTotal;

    private final EnergyStorageImpl energy;
    private final EnergyStorage energyInputView;
    private final EnergyStorage energyOutputView;
    private final EnergyStorage energyBothView;
    private final UpgradeContainer upgradeContainer = new UpgradeContainer();

    private static final class OutputRecipe {
        private final Item mainOutput;
        private final Item byproduct;

        private OutputRecipe(Item mainOutput, @Nullable Item byproduct) {
            this.mainOutput = mainOutput;
            this.byproduct = byproduct;
        }
    }

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

    public SeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEPARATOR, pos, state);
        BillTechConfig.InorganicSeparator cfg = BillTechConfig.get().inorganicSeparator;
        energyCapacity = cfg.energyCapacity;
        energyPerTick = cfg.energyPerTick;
        ticksPerItem = cfg.ticksPerItem;
        outputPerItem = cfg.outputPerItem;
        slagChance = cfg.slagChance;
        cookTimeTotal = ticksPerItem;
        energy = new EnergyStorageImpl();
        energyInputView = new EnergyStorageView(true, false);
        energyOutputView = new EnergyStorageView(false, true);
        energyBothView = new EnergyStorageView(true, true);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.NONE);
        }
        sideConfig.set(TransportType.ITEM, getInputSide(), PortMode.INPUT);
        sideConfig.set(TransportType.ITEM, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeparatorBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        if (!isAssembled(level)) {
            cookTime = 0;
            return;
        }
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            cookTime = 0;
            return;
        }
        OutputRecipe recipe = getRecipe(input);
        if (recipe == null) {
            cookTime = 0;
            return;
        }
        Item byproduct = resolveByproduct(level, recipe);
        if (!canAcceptOutput(recipe.mainOutput, byproduct)) {
            cookTime = 0;
            return;
        }
        long effectiveEnergyPerTick = getEffectiveEnergyPerTick();
        int effectiveCookTime = getEffectiveCookTime(ticksPerItem);
        if (energy.getAmount() < effectiveEnergyPerTick) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = energy.extract(effectiveEnergyPerTick, tx);
            if (extracted == effectiveEnergyPerTick) {
                tx.commit();
                cookTime++;
            }
        }
        cookTimeTotal = effectiveCookTime;
        if (cookTime >= cookTimeTotal) {
            finishRecipe(recipe.mainOutput, byproduct);
            cookTime = 0;
        }
    }

    private boolean isAssembled(Level level) {
        if (!getBlockState().getValue(SeparatorBlock.ASSEMBLED)) {
            return false;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        return above.getBlock() instanceof com.billtech.block.GrinderBlock;
    }

    @Nullable
    private OutputRecipe getRecipe(ItemStack input) {
        Item item = input.getItem();
        if (item == Items.SAND || item == Items.RED_SAND || item == Items.SANDSTONE) {
            Item byproduct = (item == Items.RED_SAND || item == Items.SANDSTONE) ? ModItems.SODIUM_SALT : null;
            return new OutputRecipe(ModItems.SILICA_POWDER, byproduct);
        }
        if (item == Items.CALCITE || item == Items.DRIPSTONE_BLOCK) {
            return new OutputRecipe(ModItems.LIME_POWDER, null);
        }
        if (item == Items.CLAY || item == Items.TERRACOTTA || item == Items.MUD) {
            return new OutputRecipe(ModItems.ALUMINA_POWDER, null);
        }
        if (item == Items.STONE || item == Items.COBBLESTONE || item == Items.ANDESITE
                || item == Items.DIORITE || item == Items.GRANITE || item == Items.DEEPSLATE
                || item == Items.COBBLED_DEEPSLATE || item == Items.TUFF) {
            return new OutputRecipe(ModItems.IRON_OXIDE_POWDER, null);
        }
        if (item == Items.BASALT || item == Items.BLACKSTONE || item == Items.NETHERRACK) {
            return new OutputRecipe(ModItems.SULFUR_POWDER, null);
        }
        return null;
    }

    @Nullable
    private Item resolveByproduct(Level level, OutputRecipe recipe) {
        if (recipe.byproduct != null) {
            return recipe.byproduct;
        }
        if (level.random.nextDouble() < slagChance) {
            return ModItems.SLAG;
        }
        return null;
    }

    private boolean canAcceptOutput(Item mainOutput, @Nullable Item byproduct) {
        if (!canAcceptStack(SLOT_OUTPUT, mainOutput, outputPerItem)) {
            return false;
        }
        if (byproduct != null && !canAcceptStack(SLOT_BYPRODUCT, byproduct, 1)) {
            return false;
        }
        return true;
    }

    private boolean canAcceptStack(int slot, Item item, int count) {
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(existing, new ItemStack(item))) {
            return false;
        }
        return existing.getCount() + count <= existing.getMaxStackSize();
    }

    private void finishRecipe(Item mainOutput, @Nullable Item byproduct) {
        ItemStack main = items.get(SLOT_OUTPUT);
        if (main.isEmpty()) {
            items.set(SLOT_OUTPUT, new ItemStack(mainOutput, outputPerItem));
        } else {
            main.grow(outputPerItem);
        }
        if (byproduct != null) {
            ItemStack byproductStack = items.get(SLOT_BYPRODUCT);
            if (byproductStack.isEmpty()) {
                items.set(SLOT_BYPRODUCT, new ItemStack(byproduct, 1));
            } else {
                byproductStack.grow(1);
            }
        }
        items.get(SLOT_INPUT).shrink(1);
        setChanged();
    }

    private long getEffectiveEnergyPerTick() {
        int upgrades = getUpgradeCount(UpgradeType.POWER);
        BillTechConfig.Upgrades cfg = BillTechConfig.get().upgrades;
        double mult = 1.0 + (upgrades * cfg.powerDrawPerUpgrade * cfg.multiplier);
        return Math.max(1, Math.round(energyPerTick * mult));
    }

    private int getEffectiveCookTime(int baseTime) {
        int upgrades = getUpgradeCount(UpgradeType.SPEED);
        BillTechConfig.Upgrades cfg = BillTechConfig.get().upgrades;
        double mult = 1.0 + (upgrades * cfg.speedPerUpgrade * cfg.multiplier);
        return Math.max(1, (int) Math.round(baseTime / mult));
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

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(SeparatorBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(SeparatorBlock.FACING);
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
        tag.putInt("CookTimeTotal", cookTimeTotal);
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
        cookTime = tag.getInt("CookTime").orElse(0);
        cookTimeTotal = tag.getInt("CookTimeTotal").orElse(ticksPerItem);
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
        if (sideConfig.allowsExtract(TransportType.ITEM, direction)) {
            return SLOTS_OUTPUT;
        }
        if (sideConfig.allowsInsert(TransportType.ITEM, direction)) {
            return SLOTS_INPUT;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == SLOT_INPUT
                && (dir == null || sideConfig.allowsInsert(TransportType.ITEM, dir))
                && getRecipe(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return (slot == SLOT_OUTPUT || slot == SLOT_BYPRODUCT) && sideConfig.allowsExtract(TransportType.ITEM, dir);
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
    public Component getDisplayName() {
        return Component.translatable("container.billtech.separator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new SeparatorMenu(id, inventory, this);
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
            SeparatorBlockEntity.this.setChanged();
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
        return getBlockState().getValue(SeparatorBlock.FACING);
    }
}
