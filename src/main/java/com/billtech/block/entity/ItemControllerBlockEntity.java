package com.billtech.block.entity;

import com.billtech.block.ItemControllerBlock;
import com.billtech.block.ModBlockEntities;
import com.billtech.pipe.ItemPipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ItemControllerBlockEntity extends BlockEntity implements MenuProvider {
    private long lastSnapshotTick = -1;
    private List<ItemPipeNetwork.ItemEntry> cachedSnapshot = java.util.Collections.emptyList();

    public ItemControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_CONTROLLER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemControllerBlockEntity be) {
        be.refreshSnapshot(level);
    }

    public List<ItemPipeNetwork.ItemEntry> getSnapshot(Level level) {
        refreshSnapshot(level);
        return cachedSnapshot;
    }

    private void refreshSnapshot(Level level) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now == lastSnapshotTick) {
            return;
        }
        lastSnapshotTick = now;
        cachedSnapshot = ItemPipeNetwork.collectItems(level, worldPosition);
    }

    public long extractFromNetwork(Level level, ItemVariant variant, long amount) {
        if (level == null) {
            return 0;
        }
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx =
                     net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long extracted = ItemPipeNetwork.extractFromNetwork(level, worldPosition, variant, amount, tx, null);
            if (extracted > 0) {
                tx.commit();
            }
            return extracted;
        }
    }

    public int requestCraft(Level level, ItemVariant variant, int amount) {
        if (level == null || variant == null || variant.isBlank() || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        int crafted = 0;
        for (AutoCrafterBlockEntity crafter : ItemPipeNetwork.findAutocrafters(level, worldPosition)) {
            if (!crafter.matchesOutput(level, variant)) {
                continue;
            }
            int made = crafter.craftByItems(level, remaining);
            crafted += made;
            remaining -= made;
            if (remaining <= 0) {
                break;
            }
        }
        return crafted;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.item_controller");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.billtech.menu.ItemControllerMenu(id, inventory, this);
    }

    public Direction getFacing() {
        return getBlockState().getValue(ItemControllerBlock.FACING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
    }
}
