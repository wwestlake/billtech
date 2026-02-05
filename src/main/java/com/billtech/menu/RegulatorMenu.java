package com.billtech.menu;

import com.billtech.block.entity.RegulatorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public class RegulatorMenu extends AbstractContainerMenu {
    private final RegulatorBlockEntity regulator;
    private final ContainerData data;

    public RegulatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public RegulatorMenu(int containerId, Inventory inventory, RegulatorBlockEntity regulator) {
        super(ModMenus.REGULATOR, containerId);
        this.regulator = regulator;
        this.data = new ContainerData() {
            private int cached = 50;

            @Override
            public int get(int index) {
                if (RegulatorMenu.this.regulator == null) {
                    return cached;
                }
                return RegulatorMenu.this.regulator.getTargetPercent();
            }

            @Override
            public void set(int index, int value) {
                cached = value;
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        addDataSlots(data);
    }

    public int getTargetPercent() {
        return data.get(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (regulator != null && !player.level().isClientSide) {
            regulator.setTargetPercent(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
