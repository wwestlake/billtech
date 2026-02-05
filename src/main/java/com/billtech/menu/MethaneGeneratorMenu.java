package com.billtech.menu;

import com.billtech.block.entity.MethaneGeneratorBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class MethaneGeneratorMenu extends MachineMenuBase {
    private final Container ghostContainer;
    private final ContainerData data;
    private final MethaneGeneratorBlockEntity blockEntity;

    public MethaneGeneratorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), null);
    }

    public MethaneGeneratorMenu(int id, Inventory inventory, MethaneGeneratorBlockEntity be) {
        this(id, inventory, new SimpleContainer(1), be);
    }

    private MethaneGeneratorMenu(
            int id,
            Inventory inventory,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.METHANE_GENERATOR, id, access, EnumSet.of(TransportType.FLUID, TransportType.ENERGY));
        this.ghostContainer = ghostContainer;
        this.blockEntity = access instanceof MethaneGeneratorBlockEntity generator ? generator : null;
        this.data = new ContainerData() {
            private final int[] cache = new int[6];

            @Override
            public int get(int index) {
                if (blockEntity == null || inventory.player.level().isClientSide) {
                    return cache[index];
                }
                return switch (index) {
                    case 0 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEnergyAmount());
                    case 1 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEnergyCapacity());
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getInputAmount());
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getInputBuffer());
                    case 4 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getMethanePerTick());
                    case 5 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEnergyPerTick());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                cache[index] = value;
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
        addDataSlots(data);
        ghostContainer.setItem(0, new ItemStack(ModFluids.METHANE_BUCKET));
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new GhostSlot(ghostContainer, 0, 80, 35));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public long getEnergyAmount() {
        return Integer.toUnsignedLong(data.get(0));
    }

    public long getEnergyCapacity() {
        return Integer.toUnsignedLong(data.get(1));
    }

    public long getInputAmount() {
        return Integer.toUnsignedLong(data.get(2));
    }

    public long getInputBuffer() {
        return Integer.toUnsignedLong(data.get(3));
    }

    public long getMethanePerTick() {
        return Integer.toUnsignedLong(data.get(4));
    }

    public long getEnergyPerTick() {
        return Integer.toUnsignedLong(data.get(5));
    }
}
