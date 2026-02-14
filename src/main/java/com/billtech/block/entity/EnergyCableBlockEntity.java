package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.cover.CoverProvider;
import com.billtech.cover.CoverStorage;
import com.billtech.energy.EnergyCableNetwork;
import com.billtech.stripe.StripeCarrier;
import com.billtech.stripe.StripeData;
import com.billtech.stripe.StripeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;

public class EnergyCableBlockEntity extends BlockEntity implements CoverProvider, StripeCarrier {
    private final CoverStorage covers = new CoverStorage();
    private StripeData stripes = StripeData.EMPTY;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity be) {
        EnergyCableNetwork.tick(level, pos);
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
        stripes.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        covers.load(tag);
        stripes = StripeData.load(tag);
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

    @Override
    public StripeData getStripeData() {
        return stripes;
    }

    @Override
    public void setStripeData(StripeData data) {
        StripeData next = data == null ? StripeData.EMPTY : data;
        if (stripes.signature() == next.signature()) {
            return;
        }
        stripes = next;
        setChanged();
        if (level != null) {
            StripeUtil.notifyStripeChanged(level, worldPosition);
        }
    }

    private void markCoverDirty() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
