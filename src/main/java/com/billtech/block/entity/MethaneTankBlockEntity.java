package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MethaneTankBlockEntity extends BlockEntity implements MenuProvider {
    private final long capacity;
    private final TankStorage storage = new TankStorage();

    private final class TankStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return capacity;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            return variant.getFluid() == ModFluids.METHANE
                    && (this.variant == null || this.variant.isBlank() || this.variant.equals(variant));
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            return this.variant != null && this.variant.equals(variant);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }

    public MethaneTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METHANE_TANK, pos, state);
        capacity = BillTechConfig.get().methaneTank.capacity;
    }

    public SingleVariantStorage<FluidVariant> getStorage() {
        return storage;
    }

    public long getAmount() {
        return storage.getAmount();
    }

    public long getCapacity() {
        return capacity;
    }

    public FluidVariant getFluid() {
        return storage.getResource();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.methane_tank");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.MethaneTankMenu(id, inventory, this);
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
}
