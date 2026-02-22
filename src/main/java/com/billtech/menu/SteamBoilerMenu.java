package com.billtech.menu;

import com.billtech.block.entity.SideConfigAccess;
import com.billtech.block.entity.SteamBoilerBlockEntity;
import com.billtech.transport.TransportType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

public class SteamBoilerMenu extends MachineMenuBase {
    private final Container machineContainer;
    private final ContainerData data;
    private final SteamBoilerBlockEntity blockEntity;

    public SteamBoilerMenu(int id, Inventory inventory) {
        this(id, inventory, new net.minecraft.world.SimpleContainer(1), null);
    }

    public SteamBoilerMenu(int id, Inventory inventory, SteamBoilerBlockEntity be) {
        this(id, inventory, be, be);
    }

    private SteamBoilerMenu(int id, Inventory inventory, Container machineContainer, SideConfigAccess access) {
        super(ModMenus.STEAM_BOILER, id, access, EnumSet.of(TransportType.FLUID, TransportType.ITEM, TransportType.ENERGY));
        this.machineContainer = machineContainer;
        this.blockEntity = access instanceof SteamBoilerBlockEntity boiler ? boiler : null;

        this.data = new ContainerData() {
            private final int[] cache = new int[19];

            @Override
            public int get(int index) {
                if (blockEntity == null || inventory.player.level().isClientSide) {
                    return cache[index];
                }
                return switch (index) {
                    case 0 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getWaterAmount());
                    case 1 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getWaterBuffer());
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getFuelAmount());
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getFuelBuffer());
                    case 4 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getSteamAmount());
                    case 5 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getSteamBuffer());
                    case 6 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getSteamPerTick());
                    case 7 -> blockEntity.getBurnTime();
                    case 8 -> blockEntity.getBurnTimeTotal();
                    case 9 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEnergyAmount());
                    case 10 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEnergyCapacityLong());
                    case 11 -> blockEntity.isMultiblockComplete() ? 1 : 0;
                    case 12 -> blockEntity.isTurbineFeedEnabled() ? 1 : 0;
                    case 13 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEngineSteamAmount());
                    case 14 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getEngineSteamCapacity());
                    case 15 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getGeneratorSteamAmount());
                    case 16 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getGeneratorSteamCapacity());
                    case 17 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getGeneratorEnergyAmount());
                    case 18 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.getGeneratorEnergyCapacity());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                cache[index] = value;
            }

            @Override
            public int getCount() {
                return 19;
            }
        };

        addDataSlots(data);
        addMachineSlots();
        addPlayerSlots(inventory, 8, 84);
    }

    private void addMachineSlots() {
        addSlot(new Slot(machineContainer, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COAL)
                        || stack.is(Items.CHARCOAL)
                        || stack.is(Items.BLAZE_ROD)
                        || stack.is(Items.COAL_BLOCK);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public long getWaterAmount() {
        return data.get(0) & 0xFFFFL;
    }

    public long getWaterBuffer() {
        return data.get(1) & 0xFFFFL;
    }

    public long getFuelAmount() {
        return data.get(2) & 0xFFFFL;
    }

    public long getFuelBuffer() {
        return data.get(3) & 0xFFFFL;
    }

    public long getSteamAmount() {
        return data.get(4) & 0xFFFFL;
    }

    public long getSteamBuffer() {
        return data.get(5) & 0xFFFFL;
    }

    public long getSteamPerTick() {
        return data.get(6) & 0xFFFFL;
    }

    public int getBurnTime() {
        return data.get(7);
    }

    public int getBurnTimeTotal() {
        return data.get(8);
    }

    public long getEnergyAmount() {
        return data.get(9) & 0xFFFFL;
    }

    public long getEnergyCapacity() {
        return data.get(10) & 0xFFFFL;
    }

    public boolean isMultiblockComplete() {
        return data.get(11) != 0;
    }

    public boolean isTurbineFeedEnabled() {
        return data.get(12) != 0;
    }

    public long getEngineSteamAmount() {
        return data.get(13) & 0xFFFFL;
    }

    public long getEngineSteamCapacity() {
        return data.get(14) & 0xFFFFL;
    }

    public long getGeneratorSteamAmount() {
        return data.get(15) & 0xFFFFL;
    }

    public long getGeneratorSteamCapacity() {
        return data.get(16) & 0xFFFFL;
    }

    public long getGeneratorEnergyAmount() {
        return data.get(17) & 0xFFFFL;
    }

    public long getGeneratorEnergyCapacity() {
        return data.get(18) & 0xFFFFL;
    }
}
