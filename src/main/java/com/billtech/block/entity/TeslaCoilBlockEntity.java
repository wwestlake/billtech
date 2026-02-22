package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.menu.TeslaCoilMenu;
import com.billtech.transport.TransportType;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class TeslaCoilBlockEntity extends BlockEntity implements SideConfigAccess, MenuProvider, MachineStatusAccess, RemoteControllable {
    private static final long ENERGY_CAPACITY = 200_000;
    private static final long ENERGY_RECEIVE = 4_000;
    private static final long ENERGY_PER_ZAP = 3_000;
    private static final int ZAP_COOLDOWN = 20;
    private static final int RANGE = 12;
    private static final int SYNTHETIC_CHARGE_TICKS = 100;

    private int cooldown;
    private boolean remoteEnabled = true;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);
    private final EnergyStorageImpl energy = new EnergyStorageImpl();

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

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_COIL, pos, state);
        for (Direction dir : Direction.values()) {
            sideConfig.set(TransportType.ENERGY, dir, PortMode.INPUT);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeslaCoilBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        pullEnergy(level);
        if (!remoteEnabled) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        ControlConductorPadBlockEntity pad = findNearestPad(level);
        if (pad == null) {
            return;
        }
        if (!energy.consume(ENERGY_PER_ZAP)) {
            return;
        }
        pad.applyTeslaZap(level, SYNTHETIC_CHARGE_TICKS);
        cooldown = ZAP_COOLDOWN;
    }

    private ControlConductorPadBlockEntity findNearestPad(Level level) {
        ControlConductorPadBlockEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;
        double rangeSq = RANGE * RANGE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = worldPosition.getX() - RANGE; x <= worldPosition.getX() + RANGE; x++) {
            for (int y = worldPosition.getY() - RANGE; y <= worldPosition.getY() + RANGE; y++) {
                for (int z = worldPosition.getZ() - RANGE; z <= worldPosition.getZ() + RANGE; z++) {
                    cursor.set(x, y, z);
                    if (cursor.distSqr(worldPosition) > rangeSq) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof ControlConductorPadBlockEntity pad) || !pad.hasCapturedMob()) {
                        continue;
                    }
                    double distance = pad.getBlockPos().distSqr(worldPosition);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        nearest = pad;
                    }
                }
            }
        }
        return nearest;
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

    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side != null && !sideConfig.allowsInsert(TransportType.ENERGY, side)) {
            return EnergyStorage.EMPTY;
        }
        return energy;
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return Direction.NORTH;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.tesla_coil");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TeslaCoilMenu(id, inventory, this);
    }

    @Override
    public int getEnergyStored() {
        return clampLong(energy.getAmount());
    }

    @Override
    public int getEnergyCapacity() {
        return clampLong(ENERGY_CAPACITY);
    }

    @Override
    public int getFluidInStored() {
        return cooldown;
    }

    @Override
    public int getFluidInCapacity() {
        return ZAP_COOLDOWN;
    }

    @Override
    public int getFluidOutStored() {
        return 0;
    }

    @Override
    public int getFluidOutCapacity() {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("Energy", energy.getAmount());
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("RemoteEnabled", remoteEnabled);
        sideConfig.save(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy.setStored(tag.getLong("Energy").orElse(0L));
        cooldown = tag.getInt("Cooldown").orElse(0);
        remoteEnabled = tag.getBoolean("RemoteEnabled").orElse(true);
        sideConfig.load(tag, provider);
    }

    @Override
    public MachineRuntimeState getRuntimeState() {
        if (!remoteEnabled) {
            return MachineRuntimeState.DISABLED;
        }
        if (cooldown > 0) {
            return MachineRuntimeState.RUNNING;
        }
        if (energy.getAmount() < ENERGY_PER_ZAP) {
            return MachineRuntimeState.NO_POWER;
        }
        if (level == null || findNearestPad(level) == null) {
            return MachineRuntimeState.NO_WORK;
        }
        return MachineRuntimeState.RUNNING;
    }

    @Override
    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }

    @Override
    public void setRemoteEnabled(boolean enabled) {
        if (remoteEnabled == enabled) {
            return;
        }
        remoteEnabled = enabled;
        setChanged();
    }
}
