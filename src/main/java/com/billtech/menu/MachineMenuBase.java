package com.billtech.menu;

import com.billtech.block.EnergyCableBlock;
import com.billtech.block.FluidPipeBlock;
import com.billtech.block.ItemPipeBlock;
import com.billtech.block.entity.PortMode;
import com.billtech.block.entity.MachineStatusAccess;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.transport.TransportType;
import com.billtech.upgrade.UpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public abstract class MachineMenuBase extends AbstractContainerMenu {
    private final SideConfigAccess sideConfigAccess;
    private final SideConfigData sideConfigData;
    private final EnumSet<TransportType> supportedTypes;
    private final MachineStatusData statusData;

    protected MachineMenuBase(
            MenuType<?> type,
            int id,
            SideConfigAccess sideConfigAccess,
            EnumSet<TransportType> supportedTypes
    ) {
        super(type, id);
        this.sideConfigAccess = sideConfigAccess;
        this.supportedTypes = supportedTypes.clone();
        this.sideConfigData = new SideConfigData(sideConfigAccess);
        addDataSlots(sideConfigData);
        MachineStatusAccess statusAccess = sideConfigAccess instanceof MachineStatusAccess cast
                ? cast
                : null;
        this.statusData = new MachineStatusData(statusAccess);
        addDataSlots(statusData);
    }

    protected void addPlayerSlots(Inventory inventory, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, startX + col * 18, hotbarY));
        }
    }

    protected void addUpgradeSlots(Container upgrades, int startX, int startY) {
        if (upgrades == null || upgrades.getContainerSize() == 0) {
            return;
        }
        int slot = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int x = startX + col * 18;
                int y = startY + row * 18;
                addSlot(new UpgradeSlot(upgrades, slot++, x, y));
            }
        }
    }

    public boolean supportsTransport(TransportType type) {
        return supportedTypes.contains(type);
    }

    public PortMode getSideMode(TransportType type, Direction dir) {
        return sideConfigData.getMode(type, dir);
    }

    public Direction getFacing() {
        return sideConfigData.getFacing();
    }

    public int getSideButtonId(TransportType type, Direction dir) {
        return sideConfigData.getIndex(type, dir);
    }

    public boolean hasStatus() {
        return statusData != null;
    }

    public int getStatusEnergyStored() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.ENERGY_STORED);
    }

    public int getStatusEnergyCapacity() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.ENERGY_CAPACITY);
    }

    public int getStatusFluidInStored() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.FLUID_IN_STORED);
    }

    public int getStatusFluidInCapacity() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.FLUID_IN_CAPACITY);
    }

    public int getStatusFluidOutStored() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.FLUID_OUT_STORED);
    }

    public int getStatusFluidOutCapacity() {
        return statusData == null ? 0 : statusData.get(MachineStatusData.FLUID_OUT_CAPACITY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (sideConfigAccess == null || id < 0 || id >= sideConfigData.getSideCount()) {
            return false;
        }
        TransportType type = TransportType.values()[id / 6];
        if (!supportsTransport(type)) {
            return false;
        }
        Direction dir = Direction.from3DDataValue(id % 6);
        sideConfigAccess.getSideConfig().cycle(type, dir);
        if (sideConfigAccess instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            be.setChanged();
            refreshNeighborConnections(be);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class UpgradeSlot extends Slot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }
    }

    private static void refreshNeighborConnections(net.minecraft.world.level.block.entity.BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        BlockState selfState = be.getBlockState();
        level.sendBlockUpdated(pos, selfState, selfState, 3);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            BlockState updatedState = neighborState;
            if (neighborState.getBlock() instanceof ItemPipeBlock itemPipe) {
                updatedState = itemPipe.updateConnections(level, neighborPos, neighborState);
            } else if (neighborState.getBlock() instanceof FluidPipeBlock fluidPipe) {
                updatedState = fluidPipe.updateConnections(level, neighborPos, neighborState);
            } else if (neighborState.getBlock() instanceof EnergyCableBlock cable) {
                updatedState = cable.updateConnections(level, neighborPos, neighborState);
            }
            if (!updatedState.equals(neighborState)) {
                level.setBlock(neighborPos, updatedState, 3);
            }
        }
    }
}
