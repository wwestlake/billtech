package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.cover.CoverProvider;
import com.billtech.cover.CoverStorage;
import com.billtech.pipe.GasPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GasPipeBlockEntity extends BlockEntity implements CoverProvider {
    private final PipeStorage[] storages = new PipeStorage[Direction.values().length + 1];
    private final net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount<FluidVariant>[] cachedExtractableBySide =
            new net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount[Direction.values().length + 1];
    private final CoverStorage covers = new CoverStorage();

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

    @Override
    public boolean hasCover(Direction side) {
        return covers.hasCover(side);
    }

    @Override
    public BlockState getCoverState(Direction side) {
        return covers.getCoverState(side);
    }

    @Override
    public void setCover(Direction side, ResourceLocation blockId) {
        covers.setCover(side, blockId);
        markCoverDirty();
    }

    @Override
    public void clearCover(Direction side) {
        covers.clearCover(side);
        markCoverDirty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        covers.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        covers.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    private void markCoverDirty() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
