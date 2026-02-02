package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FlowMeterBlockEntity extends BlockEntity {
    private long lastMoved;
    private long accumulator;

    public FlowMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_METER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FlowMeterBlockEntity be) {
        be.lastMoved = be.accumulator;
        be.accumulator = 0;
        if ((level.getGameTime() % 10L) == 0L) {
            be.syncToClient();
        }
    }

    public void recordMove(long amount) {
        if (amount <= 0) {
            return;
        }
        this.accumulator += amount;
    }

    public long getLastMoved() {
        return lastMoved;
    }

    public int getRedstoneLevel() {
        long value = Math.min(lastMoved, 15);
        return (int) value;
    }

    private void syncToClient() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("LastMoved", lastMoved);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        lastMoved = tag.getLong("LastMoved").orElse(0L);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
