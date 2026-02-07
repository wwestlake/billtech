package com.billtech.pipe;

import com.billtech.block.ItemControllerBlock;
import com.billtech.block.ItemPipeBlock;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class ItemPipeNetwork {
    private ItemPipeNetwork() {
    }

    public static void tick(Level level, BlockPos origin) {
        scanNetwork(level, origin);
    }

    public static List<ItemEntry> collectItems(Level level, BlockPos origin) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null) {
            return java.util.Collections.emptyList();
        }
        Map<ItemVariant, Long> totals = new HashMap<>();
        try (Transaction tx = Transaction.openOuter()) {
            for (Endpoint endpoint : scan.endpoints) {
                for (StorageView<ItemVariant> view : endpoint.storage) {
                    if (view.isResourceBlank()) {
                        continue;
                    }
                    long amount = view.getAmount();
                    if (amount <= 0) {
                        continue;
                    }
                    ItemVariant variant = view.getResource();
                    totals.merge(variant, amount, Long::sum);
                }
            }
        }
        List<ItemEntry> entries = new ArrayList<>();
        for (Map.Entry<ItemVariant, Long> entry : totals.entrySet()) {
            entries.add(new ItemEntry(entry.getKey(), entry.getValue()));
        }
        entries.sort((a, b) -> {
            String nameA = a.variant.getItem().builtInRegistryHolder().key().location().toString();
            String nameB = b.variant.getItem().builtInRegistryHolder().key().location().toString();
            return nameA.compareTo(nameB);
        });
        return entries;
    }

    public static net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount<ItemVariant> peekExtractable(
            Level level,
            BlockPos origin,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null) {
            return null;
        }
        try (Transaction tx = Transaction.openOuter()) {
            for (Endpoint endpoint : scan.endpoints) {
                if (excludePos != null && endpoint.neighborPos.equals(excludePos)) {
                    continue;
                }
                var found = StorageUtil.findExtractableContent(endpoint.storage, tx);
                if (found == null || found.resource() == null || found.resource().isBlank()) {
                    continue;
                }
                return found;
            }
        }
        return null;
    }

    public static long insertIntoNetwork(
            Level level,
            BlockPos origin,
            ItemVariant variant,
            long maxAmount,
            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || maxAmount <= 0 || variant == null || variant.isBlank()) {
            return 0;
        }
        long remaining = maxAmount;
        for (Endpoint endpoint : scan.endpoints) {
            if (excludePos != null && endpoint.neighborPos.equals(excludePos)) {
                continue;
            }
            long inserted = endpoint.storage.insert(variant, remaining, transaction);
            if (inserted > 0) {
                remaining -= inserted;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return maxAmount - remaining;
    }

    public static long extractFromNetwork(
            Level level,
            BlockPos origin,
            ItemVariant variant,
            long maxAmount,
            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || maxAmount <= 0 || variant == null) {
            return 0;
        }
        long remaining = maxAmount;
        if (variant.isBlank()) {
            for (Endpoint endpoint : scan.endpoints) {
                if (excludePos != null && endpoint.neighborPos.equals(excludePos)) {
                    continue;
                }
                var found = StorageUtil.findExtractableContent(endpoint.storage, transaction);
                if (found == null || found.resource() == null || found.resource().isBlank()) {
                    continue;
                }
                long extracted = endpoint.storage.extract(found.resource(), remaining, transaction);
                if (extracted > 0) {
                    remaining -= extracted;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        } else {
            for (Endpoint endpoint : scan.endpoints) {
                if (excludePos != null && endpoint.neighborPos.equals(excludePos)) {
                    continue;
                }
                long extracted = endpoint.storage.extract(variant, remaining, transaction);
                if (extracted > 0) {
                    remaining -= extracted;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }
        return maxAmount - remaining;
    }

    private static NetworkScan scanNetwork(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!isPipeLike(originState) && !(originState.getBlock() instanceof ItemControllerBlock)) {
            return null;
        }
        Set<BlockPos> pipes = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        if (originState.getBlock() instanceof ItemPipeBlock) {
            queue.add(origin);
            pipes.add(origin);
        } else {
            for (Direction dir : Direction.values()) {
                BlockPos next = origin.relative(dir);
                BlockState nextState = level.getBlockState(next);
                if (isPipeLike(nextState)) {
                    pipes.add(next);
                    queue.add(next);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (pipes.contains(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (isPipeLike(nextState)) {
                    pipes.add(next);
                    queue.add(next);
                }
            }
        }
        Map<BlockPos, Endpoint> endpointMap = new HashMap<>();
        for (BlockPos pipePos : pipes) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pipePos.relative(dir);
                if (pipes.contains(neighbor)) {
                    continue;
                }
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, neighbor, dir.getOpposite());
                if (storage == null) {
                    continue;
                }
                endpointMap.putIfAbsent(neighbor, new Endpoint(pipePos, neighbor, dir, storage));
            }
        }
        return new NetworkScan(level, pipes, new ArrayList<>(endpointMap.values()));
    }

    private static boolean isPipeLike(BlockState state) {
        return state.getBlock() instanceof ItemPipeBlock;
    }

    public record ItemEntry(ItemVariant variant, long amount) {
    }

    private record Endpoint(BlockPos pipePos, BlockPos neighborPos, Direction dir, Storage<ItemVariant> storage) {
        Endpoint {
            Objects.requireNonNull(pipePos, "pipePos");
            Objects.requireNonNull(neighborPos, "neighborPos");
            Objects.requireNonNull(dir, "dir");
            Objects.requireNonNull(storage, "storage");
        }
    }

    private record NetworkScan(
            Level level,
            Set<BlockPos> pipes,
            List<Endpoint> endpoints
    ) {
    }
}
