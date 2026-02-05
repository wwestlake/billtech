package com.billtech.menu;

import com.billtech.block.entity.MethaneTankBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

public class MethaneTankMenu extends AbstractContainerMenu {
    private final MethaneTankBlockEntity tank;
    private final ContainerData data;

    public MethaneTankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public MethaneTankMenu(int containerId, Inventory inventory, MethaneTankBlockEntity tank) {
        super(ModMenus.METHANE_TANK, containerId);
        this.tank = tank;
        this.data = new ContainerData() {
            private final int[] cache = new int[3];

            @Override
            public int get(int index) {
                if (MethaneTankMenu.this.tank == null || inventory.player.level().isClientSide) {
                    return cache[index];
                }
                long amount = MethaneTankMenu.this.tank.getAmount();
                long capacity = MethaneTankMenu.this.tank.getCapacity();
                return switch (index) {
                    case 0 -> (int) Math.min(Integer.MAX_VALUE, amount);
                    case 1 -> (int) Math.min(Integer.MAX_VALUE, capacity);
                    case 2 -> BuiltInRegistries.FLUID.getId(MethaneTankMenu.this.tank.getFluid().getFluid());
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
