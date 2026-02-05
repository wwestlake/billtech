package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.pipe.GasPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GasPipeBlockEntity extends BlockEntity {
    private final PipeStorage[] storages = new PipeStorage[Direction.values().length + 1];
    private final net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount<FluidVariant>[] cachedExtractableBySide =
            new net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount[Direction.values().length + 1];

    public GasPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_PIPE, pos, state);
    }

    public Storage<FluidVariant> getStorage(Direction side) {
        int index = side == null ? storages.length - 1 : side.ordinal();
        PipeStorage storage = storages[index];
        if (storage == null) {
            storage = new PipeStorage(side);
            storages[index] = storage;
        }
        return storage;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasPipeBlockEntity be) {
        GasPipeNetwork.tick(level, pos);
        be.refreshCache(level);
    }

    private void refreshCache(Level level) {
        if (level == null) {
            for (int i = 0; i < cachedExtractableBySide.length; i++) {
                cachedExtractableBySide[i] = null;
            }
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos exclude = worldPosition.relative(dir);
            cachedExtractableBySide[dir.ordinal()] = GasPipeNetwork.peekExtractable(level, worldPosition, exclude);
        }
        cachedExtractableBySide[cachedExtractableBySide.length - 1] =
                GasPipeNetwork.peekExtractable(level, worldPosition, null);
    }

    private final class PipeStorage implements Storage<FluidVariant> {
        private final Direction side;

        private PipeStorage(Direction side) {
            this.side = side;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (level == null || resource == null || resource.isBlank() || maxAmount <= 0) {
                return 0;
            }
            BlockPos exclude = side == null ? null : worldPosition.relative(side);
            return GasPipeNetwork.insertIntoNetwork(level, worldPosition, resource, maxAmount, transaction, exclude);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (level == null || resource == null || maxAmount <= 0) {
                return 0;
            }
            BlockPos exclude = side == null ? null : worldPosition.relative(side);
            return GasPipeNetwork.extractFromNetwork(level, worldPosition, resource, maxAmount, transaction, exclude);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            if (level == null) {
                return java.util.Collections.emptyIterator();
            }
            int index = side == null ? cachedExtractableBySide.length - 1 : side.ordinal();
            var found = cachedExtractableBySide[index];
            if (found == null || found.resource() == null || found.resource().isBlank()) {
                return java.util.Collections.emptyIterator();
            }
            Direction requestSide = side;
            net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant> view =
                    new net.fabricmc.fabric.api.transfer.v1.storage.StorageView<>() {
                        @Override
                        public boolean isResourceBlank() {
                            return found.resource().isBlank();
                        }

                        @Override
                        public FluidVariant getResource() {
                            return found.resource();
                        }

                        @Override
                        public long getAmount() {
                            return found.amount();
                        }

                        @Override
                        public long getCapacity() {
                            return found.amount();
                        }

                        @Override
                        public long extract(FluidVariant resource, long maxAmount,
                                            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
                            if (level == null || resource == null || maxAmount <= 0) {
                                return 0;
                            }
                            BlockPos exclude = requestSide == null ? null : worldPosition.relative(requestSide);
                            return GasPipeNetwork.extractFromNetwork(level, worldPosition, resource, maxAmount, transaction, exclude);
                        }
                    };
            return java.util.Collections.singleton(view).iterator();
        }
    }
}
