package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.block.RegulatorBlock;
import com.billtech.menu.RegulatorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RegulatorBlockEntity extends BlockEntity implements MenuProvider {
    private int targetPercent = 50;

    public RegulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGULATOR, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RegulatorBlockEntity be) {
        int fillPercent = be.getAdjacentTankFillPercent(level, pos);
        boolean open = fillPercent < be.targetPercent;
        if (state.getValue(RegulatorBlock.OPEN) != open) {
            level.setBlock(pos, state.setValue(RegulatorBlock.OPEN, open), 3);
        }
    }

    private int getAdjacentTankFillPercent(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof TankBlockEntity tank) {
                long amount = tank.getAmount();
                long cap = TankBlockEntity.CAPACITY;
                if (cap <= 0) {
                    return 0;
                }
                return (int) Math.min(100, (amount * 100L) / cap);
            }
        }
        return 0;
    }

    public void adjustTarget(int delta) {
        int next = Math.max(0, Math.min(100, targetPercent + delta));
        if (next != targetPercent) {
            targetPercent = next;
            setChanged();
        }
    }

    public int getTargetPercent() {
        return targetPercent;
    }

    public void setTargetPercent(int value) {
        int next = Math.max(0, Math.min(100, value));
        if (next != targetPercent) {
            targetPercent = next;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("TargetPercent", targetPercent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        Integer stored = tag.getInt("TargetPercent").orElse(null);
        if (stored != null) {
            targetPercent = stored;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.regulator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new RegulatorMenu(id, inventory, this);
    }
}
