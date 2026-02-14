package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.item.ModItems;
import com.billtech.stripe.StripeData;
import com.billtech.stripe.StripeItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StripeBenchBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_PLASTIC = 1;
    private static final int SLOT_DYE1 = 2;
    private static final int SLOT_DYE2 = 3;
    private static final int SLOT_DYE3 = 4;
    private static final int SLOT_OUTPUT = 5;
    private static final int[] INPUT_SLOTS = new int[]{SLOT_INPUT, SLOT_PLASTIC, SLOT_DYE1, SLOT_DYE2, SLOT_DYE3};
    private static final int[] OUTPUT_SLOTS = new int[]{SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);

    public StripeBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRIPE_BENCH, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StripeBenchBlockEntity be) {
        be.process();
    }

    private void process() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || !isStripeCapable(input)) {
            return;
        }
        ItemStack plastic = items.get(SLOT_PLASTIC);
        if (plastic.isEmpty() || !plastic.is(ModItems.PLASTIC_SHEET)) {
            return;
        }
        StripeData stripes = buildStripeData();
        if (stripes.isEmpty()) {
            return;
        }
        int batch = Math.min(4, input.getCount());
        ItemStack outputCandidate = StripeItemData.write(input.copyWithCount(batch), stripes);

        ItemStack output = items.get(SLOT_OUTPUT);
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, outputCandidate)) {
                return;
            }
            if (output.getCount() + outputCandidate.getCount() > output.getMaxStackSize()) {
                return;
            }
            output.grow(outputCandidate.getCount());
        } else {
            items.set(SLOT_OUTPUT, outputCandidate);
        }

        input.shrink(batch);
        plastic.shrink(1);
        consumeDyes(stripes.stripeCount());
        setChanged();
    }

    public static boolean isStripeCapable(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        return block instanceof com.billtech.block.ItemPipeBlock
                || block instanceof com.billtech.block.FluidPipeBlock
                || block instanceof com.billtech.block.GasPipeBlock
                || block instanceof com.billtech.block.EnergyCableBlock;
    }

    private StripeData buildStripeData() {
        int[] colors = new int[]{0, 0, 0};
        int count = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack dye = items.get(SLOT_DYE1 + i);
            if (dye.isEmpty()) {
                break;
            }
            if (!(dye.getItem() instanceof DyeItem dyeItem)) {
                break;
            }
            colors[count] = dyeItem.getDyeColor().getId();
            count++;
        }
        return new StripeData(count, colors);
    }

    private void consumeDyes(int count) {
        for (int i = 0; i < count && i < 3; i++) {
            ItemStack dye = items.get(SLOT_DYE1 + i);
            if (!dye.isEmpty()) {
                dye.shrink(1);
            }
        }
    }

    // --- WorldlyContainer ---
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_INPUT) return isStripeCapable(stack);
        if (slot == SLOT_PLASTIC) return stack.is(ModItems.PLASTIC_SHEET);
        if (slot == SLOT_DYE1 || slot == SLOT_DYE2 || slot == SLOT_DYE3) return stack.getItem() instanceof DyeItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.stripe_bench");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.billtech.menu.StripeBenchMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
    }
}
