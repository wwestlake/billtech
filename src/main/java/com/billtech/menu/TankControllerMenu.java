package com.billtech.menu;

import com.billtech.block.entity.TankControllerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

public class TankControllerMenu extends AbstractContainerMenu {
    private final TankControllerBlockEntity controller;
    private final ContainerData data;

    public TankControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public TankControllerMenu(int containerId, Inventory inventory, TankControllerBlockEntity controller) {
        super(ModMenus.TANK_CONTROLLER, containerId);
        this.controller = controller;
        this.data = new ContainerData() {
            private final int[] cache = new int[3];

            @Override
            public int get(int index) {
                if (TankControllerMenu.this.controller == null || inventory.player.level().isClientSide) {
                    return cache[index];
                }
                TankControllerBlockEntity.Snapshot snapshot = TankControllerMenu.this.controller.computeSnapshot(inventory.player.level());
                long amount = snapshot.amount() / 81;
                long capacity = snapshot.capacity() / 81;
                return switch (index) {
                    case 0 -> (int) amount;
                    case 1 -> (int) capacity;
                    case 2 -> BuiltInRegistries.FLUID.getId(snapshot.fluid().getFluid());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                cache[index] = value;
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
        addDataSlots(data);
    }

    public long getAmount() {
        return Integer.toUnsignedLong(data.get(0));
    }

    public long getCapacity() {
        return Integer.toUnsignedLong(data.get(1));
    }

    public int getFluidId() {
        return data.get(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
