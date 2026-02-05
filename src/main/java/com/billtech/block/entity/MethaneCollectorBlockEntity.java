package com.billtech.block.entity;

import com.billtech.block.MethaneCollectorBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.config.BillTechConfig;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MethaneCollectorBlockEntity extends BlockEntity implements MenuProvider, SideConfigAccess {
    private final long outputPerTick;
    private final long outputBuffer;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);

    private final OutputStorage outputStorage = new OutputStorage();
    private final Storage<FluidVariant> outputView = new OutputStorageView();

    private final class OutputStorage extends SingleVariantStorage<FluidVariant> {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return outputBuffer;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (variant == null || variant.isBlank()) {
                return false;
            }
            FluidVariant target = FluidVariant.of(ModFluids.METHANE);
            if (!variant.equals(target)) {
                return false;
            }
            return this.variant == null || this.variant.isBlank() || this.variant.equals(variant);
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

    private final class OutputStorageView implements Storage<FluidVariant> {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return outputStorage.extract(resource, maxAmount, transaction);
        }

        @Override
        public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
            return outputStorage.iterator();
        }
    }

    public MethaneCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METHANE_COLLECTOR, pos, state);
        BillTechConfig.MethaneCollector cfg = BillTechConfig.get().methaneCollector;
        outputPerTick = cfg.outputPerTick;
        outputBuffer = cfg.outputBuffer;
        sideConfig.set(TransportType.FLUID, getOutputSide(), PortMode.OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MethaneCollectorBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        if (!isValidBiome(level)) {
            return;
        }
        tryPushOutput(level);
        if (outputStorage.getAmount() >= outputBuffer) {
            return;
        }
        FluidVariant methane = FluidVariant.of(ModFluids.METHANE);
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = outputStorage.insert(methane, outputPerTick, tx);
            if (inserted > 0) {
                tx.commit();
            }
        }
    }

    private boolean isValidBiome(Level level) {
        var biome = level.getBiome(worldPosition);
        return biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    public long getOutputAmount() {
        return outputStorage.getAmount();
    }

    public long getOutputBuffer() {
        return outputBuffer;
    }

    public long getOutputPerTick() {
        return outputPerTick;
    }

    public boolean isInValidBiome(Level level) {
        return isValidBiome(level);
    }

    private void tryPushOutput(Level level) {
        if (outputStorage.getAmount() <= 0) {
            return;
        }
        Direction out = getOutputSide();
        BlockPos neighbor = worldPosition.relative(out);
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, neighbor, out.getOpposite());
        if (target == null) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = StorageUtil.move(outputStorage, target, variant -> true, outputStorage.getAmount(), tx);
            if (moved > 0) {
                tx.commit();
            }
        }
    }

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(MethaneCollectorBlock.FACING);
        return facing.getCounterClockWise();
    }

    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side == null) {
            return outputView;
        }
        if (sideConfig.allowsExtract(TransportType.FLUID, side)) {
            return outputView;
        }
        return Storage.empty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        CompoundTag fluidTag = new CompoundTag();
        SingleVariantStorage.writeNbt(outputStorage, FluidVariant.CODEC, fluidTag, provider);
        tag.put("Output", fluidTag);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        CompoundTag fluidTag = tag.getCompound("Output").orElse(null);
        if (fluidTag != null) {
            SingleVariantStorage.readNbt(outputStorage, FluidVariant.CODEC, FluidVariant::blank, fluidTag, provider);
        }
        sideConfig.load(tag, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.methane_collector");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.MethaneCollectorMenu(id, inventory, this);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(MethaneCollectorBlock.FACING);
    }
}
