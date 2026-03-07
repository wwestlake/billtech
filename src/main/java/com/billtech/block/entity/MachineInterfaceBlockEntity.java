package com.billtech.block.entity;

import com.billtech.automation.MachineCardData;
import com.billtech.block.MachineInterfaceBlock;
import com.billtech.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.billtech.menu.MachineInterfaceMenu;

public class MachineInterfaceBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> cardSlot = NonNullList.withSize(1, ItemStack.EMPTY);

    public MachineInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_INTERFACE, pos, state);
    }

    public boolean installCard(ItemStack source) {
        if (!cardSlot.get(0).isEmpty()) {
            return false;
        }
        if (!MachineCardData.isMachineCard(source)) {
            return false;
        }
        ItemStack inserted = source.copyWithCount(1);
        String fallbackLabel = inserted.getHoverName().getString();
        MachineCardData.ensureIdentity(inserted, fallbackLabel);
        cardSlot.set(0, inserted);
        setChanged();
        return true;
    }

    public ItemStack removeCard() {
        ItemStack existing = cardSlot.get(0);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        cardSlot.set(0, ItemStack.EMPTY);
        setChanged();
        return existing;
    }

    public boolean hasCard() {
        return !cardSlot.get(0).isEmpty();
    }

    public ItemStack getCard() {
        return cardSlot.get(0);
    }

    public boolean isTargetControllable() {
        if (level == null) {
            return false;
        }
        BlockEntity target = level.getBlockEntity(getTargetPos());
        return target instanceof RemoteControllable;
    }

    public boolean isTargetRemoteEnabled() {
        if (level == null) {
            return false;
        }
        BlockEntity target = level.getBlockEntity(getTargetPos());
        if (!(target instanceof RemoteControllable controllable)) {
            return false;
        }
        return controllable.isRemoteEnabled();
    }

    public boolean setTargetRemoteEnabled(boolean enabled) {
        if (level == null) {
            return false;
        }
        BlockEntity target = level.getBlockEntity(getTargetPos());
        if (!(target instanceof RemoteControllable controllable)) {
            return false;
        }
        controllable.setRemoteEnabled(enabled);
        target.setChanged();
        setChanged();
        return true;
    }

    public int getTargetBlockId() {
        if (level == null) {
            return 0;
        }
        BlockEntity target = level.getBlockEntity(getTargetPos());
        if (target == null) {
            return 0;
        }
        return BuiltInRegistries.BLOCK.getId(target.getBlockState().getBlock());
    }

    public String getCardLabel() {
        if (!hasCard()) {
            return "";
        }
        return MachineCardData.getLabel(cardSlot.get(0));
    }

    public BlockPos getTargetPos() {
        var state = getBlockState();
        if (!state.hasProperty(MachineInterfaceBlock.FACING)) {
            return worldPosition;
        }
        return worldPosition.relative(state.getValue(MachineInterfaceBlock.FACING));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, cardSlot, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, cardSlot, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.machine_interface");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineInterfaceMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return cardSlot.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return cardSlot.get(0);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.removeItem(cardSlot, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(cardSlot, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        if (!stack.isEmpty() && MachineCardData.isMachineCard(stack)) {
            MachineCardData.ensureIdentity(stack, stack.getHoverName().getString());
        }
        cardSlot.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        cardSlot.set(0, ItemStack.EMPTY);
        setChanged();
    }
}
