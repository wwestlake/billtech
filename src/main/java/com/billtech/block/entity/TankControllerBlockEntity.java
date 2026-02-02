package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.menu.TankControllerMenu;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TankControllerBlockEntity extends BlockEntity implements MenuProvider {
    private long lastAmount;
    private long lastCapacity;
    private FluidVariant lastFluid = FluidVariant.blank();
    private int lastRedstoneLevel;

    public TankControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK_CONTROLLER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankControllerBlockEntity be) {
        be.refreshSnapshot(level);
    }

    public long getLastAmount() {
        return lastAmount;
    }

    public long getLastCapacity() {
        return lastCapacity;
    }

    public FluidVariant getLastFluid() {
        return lastFluid;
    }

    public Snapshot computeSnapshot(Level level) {
        NetworkSnapshot snapshot = scanTankNetwork(level, getBlockPos());
        return new Snapshot(snapshot.fluid, snapshot.amount, snapshot.capacity);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.tank_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TankControllerMenu(containerId, inventory, this);
    }

    private void refreshSnapshot(Level level) {
        NetworkSnapshot snapshot = scanTankNetwork(level, getBlockPos());
        this.lastAmount = snapshot.amount;
        this.lastCapacity = snapshot.capacity;
        this.lastFluid = snapshot.fluid;
        int levelValue = computeRedstoneLevel();
        if (levelValue != lastRedstoneLevel) {
            lastRedstoneLevel = levelValue;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
        setChanged();
    }

    public int getRedstoneLevel() {
        return lastRedstoneLevel;
    }

    private int computeRedstoneLevel() {
        if (lastCapacity <= 0) {
            return 0;
        }
        return (int) Math.min(15, (lastAmount * 15) / lastCapacity);
    }

    private NetworkSnapshot scanTankNetwork(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        long amount = 0;
        long capacity = 0;
        FluidVariant fluid = FluidVariant.blank();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof TankBlockEntity tank) {
                    visited.add(next);
                    queue.add(next);
                    SingleVariantStorage<FluidVariant> storage = tank.getStorage();
                    capacity += TankBlockEntity.CAPACITY;
                    long stored = storage.getAmount();
                    if (stored > 0) {
                        FluidVariant variant = storage.getResource();
                        if (fluid.isBlank()) {
                            fluid = variant;
                        }
                        if (fluid.equals(variant)) {
                            amount += stored;
                        }
                    }
                }
            }
        }
        return new NetworkSnapshot(fluid, amount, capacity);
    }

    private record NetworkSnapshot(FluidVariant fluid, long amount, long capacity) {
    }

    public record Snapshot(FluidVariant fluid, long amount, long capacity) {
    }
}
