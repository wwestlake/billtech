package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TankBlockEntity extends BlockEntity {
    public static final long CAPACITY = 810_000;

    private final TankStorage storage = new TankStorage();

    private final class TankStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return CAPACITY;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant.isBlank()) {
                return false;
            }
            if (this.variant != null && !this.variant.isBlank()) {
                return this.variant.equals(variant);
            }
            if (level == null) {
                return true;
            }
            FluidVariant networkFluid = findNetworkFluid(level, worldPosition);
            return networkFluid.isBlank() || networkFluid.equals(variant);
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            if (variant.isBlank()) {
                return false;
            }
            if (this.variant == null || this.variant.isBlank()) {
                return false;
            }
            return this.variant.equals(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
            syncToClient();
        }
        void setRaw(FluidVariant variant, long amount) {
            this.variant = variant;
            this.amount = amount;
            setChanged();
            syncToClient();
        }
    }

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK_BLOCK, pos, state);
    }

    public SingleVariantStorage<FluidVariant> getStorage() {
        return storage;
    }

    public Storage<FluidVariant> getNetworkStorage() {
        if (level == null) {
            return storage;
        }
        NetworkScan scan = scanNetwork(level, worldPosition);
        if (scan == null || scan.tanks.isEmpty()) {
            return storage;
        }
        ArrayList<TankStorage> storages = new ArrayList<>(scan.tanks.size());
        for (TankBlockEntity tank : scan.tanks) {
            storages.add(tank.storage);
        }
        return new CombinedStorage<>(storages);
    }

    void setContents(FluidVariant variant, long amount) {
        storage.setRaw(variant, amount);
    }

    public long getAmount() {
        return storage.getAmount();
    }

    public FluidVariant getFluid() {
        return storage.getResource();
    }

    private void syncToClient() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        SingleVariantStorage.writeNbt(storage, FluidVariant.CODEC, tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        SingleVariantStorage.readNbt(storage, FluidVariant.CODEC, FluidVariant::blank, tag, provider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity be) {
        NetworkScan scan = scanNetwork(level, pos);
        if (scan == null || !scan.controller.equals(pos)) {
            return;
        }
        if (scan.fluidConflict) {
            return;
        }
        if (scan.totalCapacity <= 0) {
            return;
        }
        long remaining = scan.totalAmount;
        for (TankBlockEntity tank : scan.tanks) {
            long cap = tank.storage.getCapacity(FluidVariant.blank());
            long assigned = Math.min(cap, remaining);
            if (assigned > 0) {
                tank.setContents(scan.fluid, assigned);
                remaining -= assigned;
            } else {
                tank.setContents(FluidVariant.blank(), 0);
            }
        }
    }

    private static NetworkScan scanNetwork(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!(level.getBlockEntity(origin) instanceof TankBlockEntity)) {
            return null;
        }
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        ArrayList<TankBlockEntity> tanks = new ArrayList<>();
        visited.add(origin);
        queue.add(origin);
        BlockPos controller = origin;
        long controllerKey = origin.asLong();
        FluidVariant fluid = FluidVariant.blank();
        boolean fluidConflict = false;
        long totalAmount = 0;
        long totalCapacity = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof TankBlockEntity tank)) {
                continue;
            }
            tanks.add(tank);
            totalCapacity += CAPACITY;
            long amount = tank.storage.getAmount();
            if (amount > 0) {
                FluidVariant variant = tank.storage.getResource();
                if (fluid.isBlank()) {
                    fluid = variant;
                } else if (!fluid.equals(variant)) {
                    fluidConflict = true;
                }
                if (!fluidConflict && fluid.equals(variant)) {
                    totalAmount += amount;
                }
            }
            long key = pos.asLong();
            if (key < controllerKey) {
                controllerKey = key;
                controller = pos;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof TankBlockEntity) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return new NetworkScan(controller, tanks, fluid, totalAmount, totalCapacity, fluidConflict);
    }

    private static FluidVariant findNetworkFluid(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        FluidVariant found = FluidVariant.blank();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TankBlockEntity tank) {
                long amount = tank.storage.getAmount();
                if (amount > 0) {
                    FluidVariant variant = tank.storage.getResource();
                    if (!variant.isBlank()) {
                        if (found.isBlank()) {
                            found = variant;
                        } else if (!found.equals(variant)) {
                            return FluidVariant.blank();
                        }
                    }
                }
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof TankBlockEntity) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return found;
    }

    private record NetworkScan(
            BlockPos controller,
            ArrayList<TankBlockEntity> tanks,
            FluidVariant fluid,
            long totalAmount,
            long totalCapacity,
            boolean fluidConflict
    ) {
    }
}
