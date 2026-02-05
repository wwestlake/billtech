package com.billtech.menu;

import com.billtech.block.entity.MethaneCollectorBlockEntity;
import com.billtech.block.entity.SideConfigAccess;
import com.billtech.fluid.ModFluids;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class MethaneCollectorMenu extends MachineMenuBase {
    private final Container ghostContainer;
    private final ContainerData data;
    private final MethaneCollectorBlockEntity blockEntity;

    public MethaneCollectorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), null);
    }

    public MethaneCollectorMenu(int id, Inventory inventory, MethaneCollectorBlockEntity be) {
        this(id, inventory, new SimpleContainer(1), be);
    }

    private MethaneCollectorMenu(
            int id,
            Inventory inventory,
            Container ghostContainer,
            SideConfigAccess access
    ) {
        super(ModMenus.METHANE_COLLECTOR, id, access, EnumSet.of(TransportType.FLUID));
        this.ghostContainer = ghostContainer;
        this.blockEntity = access instanceof MethaneCollectorBlockEntity collector ? collector : null;
        this.data = new ContainerData() {
            private final int[] cache = new int[4];

            @Override
            public int get(int index) {
                if (blockEntity == null || inventory.player.level().isClientSide) {
                    return cache[index];
                }
                return switch (index) {
                    case 0 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getOutputAmount());
                    case 1 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getOutputBuffer());
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getOutputPerTick());
                    case 3 -> blockEntity.isInValidBiome(inventory.player.level()) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                cache[index] = value;
            }

            @Override
            public int getCount() {
                return 4;
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

    public long getOutputAmount() {
        return Integer.toUnsignedLong(data.get(0));
    }

    public long getOutputBuffer() {
        return Integer.toUnsignedLong(data.get(1));
    }

    public long getOutputPerTick() {
        return Integer.toUnsignedLong(data.get(2));
    }

    public boolean isInValidBiome() {
        return data.get(3) != 0;
    }
}
