package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.ModBlocks;
import com.billtech.block.SentryControllerBlock;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.UUID;

public class SentryControllerBlockEntity extends BlockEntity implements SideConfigAccess {
    private static final long ENERGY_CAPACITY = 150_000;
    private static final long ENERGY_RECEIVE = 2_000;
    private static final long ENERGY_PER_TICK = 80;
    private static final long ESSENCE_CAPACITY = 20_000;
    private static final long ESSENCE_PER_TICK = 15;

    private UUID owner;
    private boolean structureValid;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);

    private final EnergyStorageImpl energy = new EnergyStorageImpl();
    private final EssenceStorage essenceStorage = new EssenceStorage();

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

        private void setStored(long amount) {
            this.amount = Math.min(ENERGY_CAPACITY, Math.max(0, amount));
        }
    }

    private final class EssenceStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return ESSENCE_CAPACITY;
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

    public SentryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SENTRY_CONTROLLER, pos, state);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.FLUID, dir, PortMode.NONE);
            sideConfig.set(TransportType.ENERGY, dir, PortMode.NONE);
        }
        sideConfig.set(TransportType.FLUID, getFacing().getCounterClockWise(), PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, Direction.DOWN, PortMode.INPUT);
        sideConfig.set(TransportType.ENERGY, getFacing().getOpposite(), PortMode.INPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SentryControllerBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel level) {
        pullEnergy(level);
        structureValid = hasValidStack(level);
        BlockEntity containerBe = level.getBlockEntity(worldPosition.above());
        if (!(containerBe instanceof SentryContainerBlockEntity container)) {
            return;
        }
        if (!structureValid) {
            container.maintainGuard(level, false, effectiveOwner());
            return;
        }
        boolean powered = energy.consume(ENERGY_PER_TICK) && consumeEssence();
        container.maintainGuard(level, powered, effectiveOwner());
    }

    private UUID effectiveOwner() {
        return owner == null ? new UUID(0L, 0L) : owner;
    }

    private boolean hasValidStack(Level level) {
        return level.getBlockState(worldPosition.above()).is(ModBlocks.SENTRY_CONTAINER)
                && level.getBlockState(worldPosition.above(2)).is(ModBlocks.SENTRY_COVER);
    }

    private boolean consumeEssence() {
        if (ModFluids.MOB_ESSENCE == null) {
            return false;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = essenceStorage.extract(FluidVariant.of(ModFluids.MOB_ESSENCE), ESSENCE_PER_TICK, tx);
            if (extracted < ESSENCE_PER_TICK) {
                return false;
            }
            tx.commit();
            return true;
        }
    }

    private void pullEnergy(Level level) {
        long need = ENERGY_CAPACITY - energy.getAmount();
        if (need <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!sideConfig.allowsInsert(TransportType.ENERGY, dir)) {
                continue;
            }
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

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        PortMode mode = sideConfig.get(TransportType.ENERGY, side);
        if (!mode.allowsInsert()) {
            return EnergyStorage.EMPTY;
        }
        return energy;
    }

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side != null) {
            PortMode mode = sideConfig.get(TransportType.FLUID, side);
            if (!mode.allowsInsert()) {
                return Storage.empty();
            }
        }
        return essenceStorage;
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(SentryControllerBlock.FACING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energy.getAmount());
        tag.putBoolean("StructureValid", structureValid);
        if (owner != null) {
            tag.putString("Owner", owner.toString());
        }
        CompoundTag essenceTag = new CompoundTag();
        SingleVariantStorage.writeNbt(essenceStorage, FluidVariant.CODEC, essenceTag, provider);
        tag.put("Essence", essenceTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy.setStored(tag.getLong("Energy").orElse(0L));
        structureValid = tag.getBoolean("StructureValid").orElse(false);
        String ownerStr = tag.getString("Owner").orElse("");
        owner = ownerStr.isEmpty() ? null : UUID.fromString(ownerStr);
        CompoundTag essenceTag = tag.getCompound("Essence").orElse(null);
        if (essenceTag != null) {
            SingleVariantStorage.readNbt(essenceStorage, FluidVariant.CODEC, FluidVariant::blank, essenceTag, provider);
        }
        sideConfig.load(tag, provider);
    }
}
