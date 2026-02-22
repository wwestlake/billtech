package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.EssenceExtractorBlock;
import com.billtech.fluid.ModFluids;
import com.billtech.upgrade.UpgradeItem;
import com.billtech.upgrade.UpgradeType;
import com.billtech.pipe.ItemPipeNetwork;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import com.billtech.menu.EssenceExtractorMenu;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class EssenceExtractorBlockEntity extends BlockEntity implements WorldlyContainer, SideConfigAccess, MenuProvider, MachineStatusAccess {
    private static final long ENERGY_CAPACITY = 120_000;
    private static final long ENERGY_RECEIVE = 2_000;
    private static final long ENERGY_PER_TICK = 120;
    private static final long ESSENCE_BUFFER = 40_000;
    private static final long BASE_ESSENCE_YIELD = 350;
    private static final long BONUS_ESSENCE_PER_HEALTH_MISSING = 35;
    private static final int PROCESS_TICKS = 40;
    private static final int SPEED_MAX = 4;
    private static final int MULTI_KILL_MAX = 2;
    private static final int YIELD_MAX = 3;
    private static final double SPEED_TICK_MULTIPLIER = 1.25d;
    private static final double SPEED_TIME_MULTIPLIER = 0.80d;
    private static final double MULTI_KILL_ENERGY_PER_EXTRA = 0.80d;
    private static final double YIELD_PER_UPGRADE = 0.12d;
    private static final double YIELD_CAP = 0.40d;
    private static final int[] OUTPUT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

    private int process;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final NonNullList<net.minecraft.world.item.ItemStack> outputItems = NonNullList.withSize(9, net.minecraft.world.item.ItemStack.EMPTY);
    private final NonNullList<net.minecraft.world.item.ItemStack> upgrades = NonNullList.withSize(3, net.minecraft.world.item.ItemStack.EMPTY);
    private final UpgradeContainer upgradeContainer = new UpgradeContainer();

    private final EnergyStorageImpl energy = new EnergyStorageImpl();
    private final OutputStorage essenceStorage = new OutputStorage();
    private final EnergyStorage energyInputView = new EnergyStorage() {
        @Override
        public long insert(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            return energy.insert(maxAmount, transaction);
        }

        @Override
        public long extract(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            return 0;
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
            return true;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    };

    private final class EnergyStorageImpl extends SimpleEnergyStorage {
        private EnergyStorageImpl() {
            super(ENERGY_CAPACITY, ENERGY_RECEIVE, 0);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }

        private boolean consume(long amount) {
            if (this.amount < amount) {
                return false;
            }
            this.amount -= amount;
            setChanged();
            return true;
        }

        private long stored() {
            return this.amount;
        }

        private void setStored(long amount) {
            this.amount = Math.min(ENERGY_CAPACITY, Math.max(0, amount));
        }
    }

    private final class OutputStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return ESSENCE_BUFFER;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            if (ModFluids.MOB_ESSENCE == null) {
                return false;
            }
            return variant.getFluid() == ModFluids.MOB_ESSENCE
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

    public EssenceExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_EXTRACTOR, pos, state);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ITEM, dir, PortMode.NONE);
            sideConfig.set(TransportType.FLUID, dir, PortMode.NONE);
            sideConfig.set(TransportType.ENERGY, dir, PortMode.NONE);
        }
        sideConfig.set(TransportType.ITEM, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.FLUID, getOutputSide(), PortMode.OUTPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, getFacing().getOpposite(), PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceExtractorBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        pullEnergy(level);
        pushEssence(level);
        if (ModFluids.MOB_ESSENCE == null) {
            process = 0;
            return;
        }
        List<LivingEntity> targets = findTargets(level);
        if (targets.isEmpty()) {
            process = 0;
            return;
        }
        if (essenceStorage.getAmount() >= ESSENCE_BUFFER) {
            process = 0;
            return;
        }
        if (!energy.consume(getEffectiveEnergyPerTick())) {
            return;
        }
        process++;
        if (process < getEffectiveProcessTicks()) {
            return;
        }
        process = 0;
        extractTargets(level);
    }

    private List<LivingEntity> findTargets(Level level) {
        AABB chamber = new AABB(
                worldPosition.getX() + 0.05,
                worldPosition.getY() + 0.80,
                worldPosition.getZ() + 0.05,
                worldPosition.getX() + 0.95,
                worldPosition.getY() + 2.60,
                worldPosition.getZ() + 0.95
        );
        return level.getEntitiesOfClass(LivingEntity.class, chamber, this::isValidTarget);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return false;
        }
        return true;
    }

    private void extractTargets(Level level) {
        List<LivingEntity> targets = findTargets(level);
        if (targets.isEmpty()) {
            return;
        }
        int maxTargets = getMaxTargetsPerCycle();
        int processed = 0;
        for (LivingEntity mob : targets) {
            if (processed >= maxTargets) {
                break;
            }
            if (!extractMob(level, mob)) {
                break;
            }
            processed++;
        }
    }

    private boolean extractMob(Level level, LivingEntity mob) {
        float missing = Math.max(0.0f, mob.getMaxHealth() - mob.getHealth());
        long baseYield = BASE_ESSENCE_YIELD + (long) missing * BONUS_ESSENCE_PER_HEALTH_MISSING;
        long yield = applyYieldBonus(baseYield);
        FluidVariant essence = FluidVariant.of(ModFluids.MOB_ESSENCE);
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = essenceStorage.insert(essence, yield, tx);
            if (inserted <= 0) {
                return false;
            }
            tx.commit();
        }

        BlockPos dropPos = mob.blockPosition();
        mob.hurt(level.damageSources().magic(), Float.MAX_VALUE);
        routeNearbyDrops(level, dropPos);
        clearNearbyXp(level, dropPos);
        return true;
    }

    private void routeNearbyDrops(Level level, BlockPos center) {
        AABB dropZone = new AABB(center).inflate(1.5);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, dropZone, Entity::isAlive);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.getItem().isEmpty()) {
                continue;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long moved = ItemPipeNetwork.insertIntoNetwork(
                        level,
                        worldPosition,
                        ItemVariant.of(itemEntity.getItem()),
                        itemEntity.getItem().getCount(),
                        tx,
                        null
                );
                if (moved <= 0) {
                    continue;
                }
                tx.commit();
                int left = itemEntity.getItem().getCount() - (int) moved;
                if (left <= 0) {
                    itemEntity.discard();
                } else {
                    net.minecraft.world.item.ItemStack remainder = itemEntity.getItem().copyWithCount(left);
                    net.minecraft.world.item.ItemStack leftover = addToOutput(remainder);
                    if (leftover.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(leftover);
                    }
                }
            }
        }
    }

    private net.minecraft.world.item.ItemStack addToOutput(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.ItemStack remaining = stack.copy();
        for (int i = 0; i < outputItems.size(); i++) {
            net.minecraft.world.item.ItemStack slot = outputItems.get(i);
            if (slot.isEmpty()) {
                outputItems.set(i, remaining.copy());
                setChanged();
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                remaining.shrink(move);
                if (remaining.isEmpty()) {
                    setChanged();
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            }
        }
        return remaining;
    }

    private void clearNearbyXp(Level level, BlockPos center) {
        AABB dropZone = new AABB(center).inflate(1.5);
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, dropZone, Entity::isAlive)) {
            orb.discard();
        }
    }

    private void pullEnergy(Level level) {
        long need = ENERGY_CAPACITY - energy.stored();
        if (need <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            EnergyStorage source = EnergyStorage.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (source == null) {
                continue;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long pulled = source.extract(Math.min(ENERGY_RECEIVE, need), tx);
                long inserted = energy.insert(pulled, tx);
                if (inserted > 0) {
                    tx.commit();
                    need -= inserted;
                    if (need <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void pushEssence(Level level) {
        if (essenceStorage.getAmount() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (target == null) {
                continue;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long moved = StorageUtil.move(essenceStorage, target, variant -> true, essenceStorage.getAmount(), tx);
                if (moved > 0) {
                    tx.commit();
                    if (essenceStorage.getAmount() <= 0) {
                        return;
                    }
                }
            }
        }
    }

    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        PortMode mode = sideConfig.get(TransportType.ENERGY, side);
        if (!mode.allowsInsert()) {
            return EnergyStorage.EMPTY;
        }
        return energyInputView;
    }

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side != null) {
            PortMode mode = sideConfig.get(TransportType.FLUID, side);
            if (!mode.allowsExtract()) {
                return Storage.empty();
            }
        }
        return essenceStorage;
    }

    private long getEffectiveEnergyPerTick() {
        int speed = Math.min(SPEED_MAX, getUpgradeCount(UpgradeType.SPEED));
        int multi = Math.min(MULTI_KILL_MAX, getUpgradeCount(UpgradeType.MULTI_KILL));
        double speedEnergy = Math.pow(SPEED_TICK_MULTIPLIER, speed);
        double multiEnergy = 1.0d + (multi * MULTI_KILL_ENERGY_PER_EXTRA);
        return Math.max(1L, Math.round(ENERGY_PER_TICK * speedEnergy * multiEnergy));
    }

    private int getEffectiveProcessTicks() {
        int speed = Math.min(SPEED_MAX, getUpgradeCount(UpgradeType.SPEED));
        return Math.max(1, (int) Math.ceil(PROCESS_TICKS * Math.pow(SPEED_TIME_MULTIPLIER, speed)));
    }

    private int getMaxTargetsPerCycle() {
        int multi = Math.min(MULTI_KILL_MAX, getUpgradeCount(UpgradeType.MULTI_KILL));
        return 1 + multi;
    }

    private long applyYieldBonus(long baseYield) {
        int yieldUpgrades = Math.min(YIELD_MAX, getUpgradeCount(UpgradeType.YIELD));
        double bonus = Math.min(YIELD_CAP, yieldUpgrades * YIELD_PER_UPGRADE);
        return Math.max(1L, Math.round(baseYield * (1.0d + bonus)));
    }

    public Container getUpgradeContainer() {
        return upgradeContainer;
    }

    public int getUpgradeCount(UpgradeType type) {
        int count = 0;
        for (net.minecraft.world.item.ItemStack stack : upgrades) {
            if (stack.getItem() instanceof UpgradeItem upgrade && upgrade.getType() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private Direction blockFacing() {
        return getBlockState().getValue(EssenceExtractorBlock.FACING);
    }

    private Direction getOutputSide() {
        return blockFacing().getClockWise();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energy.stored());
        tag.putInt("Process", process);
        CompoundTag fluidTag = new CompoundTag();
        SingleVariantStorage.writeNbt(essenceStorage, FluidVariant.CODEC, fluidTag, provider);
        tag.put("Essence", fluidTag);
        ContainerHelper.saveAllItems(tag, outputItems, provider);
        CompoundTag upgradesTag = new CompoundTag();
        ContainerHelper.saveAllItems(upgradesTag, upgrades, provider);
        tag.put("Upgrades", upgradesTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy.setStored(tag.getLong("Energy").orElse(0L));
        process = tag.getInt("Process").orElse(0);
        CompoundTag fluidTag = tag.getCompound("Essence").orElse(null);
        if (fluidTag != null) {
            SingleVariantStorage.readNbt(essenceStorage, FluidVariant.CODEC, FluidVariant::blank, fluidTag, provider);
        }
        ContainerHelper.loadAllItems(tag, outputItems, provider);
        CompoundTag upgradesTag = tag.getCompound("Upgrades").orElse(null);
        if (upgradesTag != null) {
            ContainerHelper.loadAllItems(upgradesTag, upgrades, provider);
        }
        sideConfig.load(tag, provider);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return blockFacing();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.essence_extractor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EssenceExtractorMenu(id, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return sideConfig.allowsExtract(TransportType.ITEM, side) ? OUTPUT_SLOTS : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, Direction dir) {
        return sideConfig.allowsExtract(TransportType.ITEM, dir);
    }

    @Override
    public int getContainerSize() {
        return outputItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (net.minecraft.world.item.ItemStack stack : outputItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem(int slot) {
        return outputItems.get(slot);
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        net.minecraft.world.item.ItemStack stack = ContainerHelper.removeItem(outputItems, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(outputItems, slot);
    }

    @Override
    public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        outputItems.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        outputItems.clear();
    }

    private final class UpgradeContainer implements Container {
        @Override
        public int getContainerSize() {
            return upgrades.size();
        }

        @Override
        public boolean isEmpty() {
            for (net.minecraft.world.item.ItemStack stack : upgrades) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public net.minecraft.world.item.ItemStack getItem(int slot) {
            return upgrades.get(slot);
        }

        @Override
        public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
            net.minecraft.world.item.ItemStack stack = ContainerHelper.removeItem(upgrades, slot, amount);
            if (!stack.isEmpty()) {
                setChanged();
            }
            return stack;
        }

        @Override
        public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(upgrades, slot);
        }

        @Override
        public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
            upgrades.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            EssenceExtractorBlockEntity.this.setChanged();
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
    public int getEnergyStored() {
        return clampLong(energy.stored());
    }

    @Override
    public int getEnergyCapacity() {
        return clampLong(ENERGY_CAPACITY);
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
        return clampLong(essenceStorage.getAmount());
    }

    @Override
    public int getFluidOutCapacity() {
        return clampLong(ESSENCE_BUFFER);
    }
}
