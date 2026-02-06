package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.transport.TransportType;
import com.billtech.upgrade.UpgradeInventoryProvider;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
import com.billtech.menu.ElectricFurnaceMenu;
import net.minecraft.world.MenuProvider;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class ElectricFurnaceBlockEntity extends BlockEntity implements WorldlyContainer, UpgradeInventoryProvider, MenuProvider, SideConfigAccess, MachineStatusAccess {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int[] SLOTS_INPUT = new int[]{SLOT_INPUT};
    private static final int[] SLOTS_OUTPUT = new int[]{SLOT_OUTPUT};
    private static final boolean DEBUG_STATUS = true;

    private final long energyCapacity;
    private final long energyPerTick;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(4, ItemStack.EMPTY);
    private int cookTime;
    private int cookTimeTotal = 200;

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

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state);
        energyCapacity = BillTechConfig.get().furnace.energyCapacity;
        energyPerTick = BillTechConfig.get().furnace.energyPerTick;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        clampEnergyToEffectiveCapacity();
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            if (DEBUG_STATUS && cookTime != 0) {
                System.out.println("[BillTech] Furnace idle: no input.");
            }
            cookTime = 0;
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace tick skipped: not server level.");
            }
            cookTime = 0;
            return;
        }
        RecipeHolder<SmeltingRecipe> recipe = serverLevel.recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .orElse(null);
        if (recipe == null) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace idle: no smelting recipe.");
            }
            cookTime = 0;
            return;
        }
        ItemStack singleInput = input.copy();
        singleInput.setCount(1);
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(singleInput), level.registryAccess());
        if (result.isEmpty()) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace idle: recipe returned empty result.");
            }
            cookTime = 0;
            return;
        }
        if (!canAcceptOutput(result)) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace idle: output blocked.");
            }
            cookTime = 0;
            return;
        }
        long effectiveEnergyPerTick = getEffectiveEnergyPerTick();
        int effectiveCookTime = getEffectiveCookTime(recipe.value().cookingTime());
        if (energy.getAmount() < effectiveEnergyPerTick) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace idle: no power. stored=" + energy.getAmount());
            }
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = energy.extract(effectiveEnergyPerTick, tx);
            if (extracted == effectiveEnergyPerTick) {
                tx.commit();
                cookTime++;
                if (DEBUG_STATUS) {
                    System.out.println("[BillTech] Furnace tick: cook=" + cookTime + "/" + cookTimeTotal +
                            " stored=" + energy.getAmount());
                }
            }
        }
        cookTimeTotal = effectiveCookTime;
        if (cookTime >= cookTimeTotal) {
            if (DEBUG_STATUS) {
                System.out.println("[BillTech] Furnace complete: input=" + input.getCount()
                        + " output=" + items.get(SLOT_OUTPUT).getCount()
                        + " result=" + result.getCount() + "x " + result.getDisplayName().getString());
            }
            finishRecipe(result);
            cookTime = 0;
        }
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void finishRecipe(ItemStack result) {
        ItemStack input = items.get(SLOT_INPUT);
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        input.shrink(1);
        setChanged();
        if (DEBUG_STATUS) {
            ItemStack outNow = items.get(SLOT_OUTPUT);
            System.out.println("[BillTech] Furnace output now=" + outNow.getCount()
                    + "x " + outNow.getDisplayName().getString() + " inputLeft=" + input.getCount());
        }
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
        cookTimeTotal = tag.getInt("CookTimeTotal").orElse(200);
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
        if (slot != SLOT_INPUT) {
            return false;
        }
        return dir == null || sideConfig.allowsInsert(TransportType.ITEM, dir);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT && sideConfig.allowsExtract(TransportType.ITEM, dir);
    }

    private Direction getInputSide() {
        Direction facing = getBlockState().getValue(com.billtech.block.ElectricFurnaceBlock.FACING);
        return facing.getClockWise();
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(com.billtech.block.ElectricFurnaceBlock.FACING);
        return facing.getCounterClockWise();
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
        return Component.translatable("container.billtech.electric_furnace");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new ElectricFurnaceMenu(id, inventory, this);
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
            ElectricFurnaceBlockEntity.this.setChanged();
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
        return getBlockState().getValue(com.billtech.block.ElectricFurnaceBlock.FACING);
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
}
