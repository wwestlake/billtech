package com.billtech.pipe;

import com.billtech.block.FluidPipeBlock;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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

public final class FluidPipeNetwork {
    private FluidPipeNetwork() {
    }

    public static void tick(Level level, BlockPos origin) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || !scan.controller.equals(origin)) {
            return;
        }
        if (scan.endpoints.isEmpty()) {
            return;
        }
        for (Endpoint source : scan.endpoints) {
            SourceInfo sourceInfo = findSourceFluid(source);
            if (sourceInfo == null) {
                continue;
            }
            if (!scan.allows(sourceInfo.variant)) {
                continue;
            }
            Map<BlockPos, Integer> distances = bfsDistances(scan.pipes, source.pipePos, scan.maxDistance);
            if (distances.isEmpty()) {
                continue;
            }
            for (Endpoint sink : scan.endpoints) {
                if (sink == source) {
                    continue;
                }
                Integer dist = distances.get(sink.pipePos);
                if (dist == null || dist > scan.maxDistance) {
                    continue;
                }
                if (!scan.allows(sourceInfo.variant)) {
                    continue;
                }
                long transferred = tryTransfer(source, sink, sourceInfo.variant, scan.maxRate);
                if (transferred > 0) {
                    return;
                }
            }
        }
    }

    private static long tryTransfer(Endpoint source, Endpoint sink, FluidVariant variant, long maxRate) {
        long sourceAvail;
        try (Transaction tx = Transaction.openOuter()) {
            sourceAvail = source.storage.extract(variant, maxRate, tx);
        }
        if (sourceAvail <= 0) {
            return 0;
        }
        long sinkAvail;
        try (Transaction tx = Transaction.openOuter()) {
            sinkAvail = sink.storage.insert(variant, maxRate, tx);
        }
        if (sinkAvail <= 0) {
            return 0;
        }
        long amount = Math.min(maxRate, Math.min(sourceAvail, sinkAvail));
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = source.storage.extract(variant, amount, tx);
            long inserted = sink.storage.insert(variant, extracted, tx);
            if (inserted == extracted && inserted > 0) {
                tx.commit();
                return inserted;
            }
        }
        return 0;
    }

    private static SourceInfo findSourceFluid(Endpoint endpoint) {
        for (StorageView<FluidVariant> view : endpoint.storage) {
            if (view.isResourceBlank()) {
                continue;
            }
            long amount = view.getAmount();
            if (amount <= 0) {
                continue;
            }
            FluidVariant variant = view.getResource();
            return new SourceInfo(variant);
        }
        return null;
    }

    private static Map<BlockPos, Integer> bfsDistances(Set<BlockPos> pipes, BlockPos start, int maxDistance) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        distances.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int dist = distances.get(current);
            if (dist >= maxDistance) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!pipes.contains(next)) {
                    continue;
                }
                if (distances.containsKey(next)) {
                    continue;
                }
                distances.put(next, dist + 1);
                queue.add(next);
            }
        }
        return distances;
    }

    private static NetworkScan scanNetwork(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof FluidPipeBlock)) {
            return null;
        }
        Set<BlockPos> pipes = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        pipes.add(origin);
        long minRate = FluidPipeTiers.maxRate(originState);
        int minDistance = FluidPipeTiers.maxDistance(originState);
        boolean waterOnly = !FluidPipeTiers.allows(originState, FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA));
        BlockPos controller = origin;
        long controllerKey = origin.asLong();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            minRate = Math.min(minRate, FluidPipeTiers.maxRate(state));
            minDistance = Math.min(minDistance, FluidPipeTiers.maxDistance(state));
            if (!FluidPipeTiers.allows(state, FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA))) {
                waterOnly = true;
            }
            long key = pos.asLong();
            if (key < controllerKey) {
                controllerKey = key;
                controller = pos;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (pipes.contains(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof FluidPipeBlock) {
                    pipes.add(next);
                    queue.add(next);
                }
            }
        }
        List<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos pipePos : pipes) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pipePos.relative(dir);
                if (pipes.contains(neighbor)) {
                    continue;
                }
                Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, neighbor, dir.getOpposite());
                if (storage == null) {
                    continue;
                }
                endpoints.add(new Endpoint(pipePos, neighbor, dir, storage));
            }
        }
        return new NetworkScan(pipes, endpoints, controller, minRate, minDistance, waterOnly);
    }

    private record SourceInfo(FluidVariant variant) {
    }

    private record Endpoint(BlockPos pipePos, BlockPos neighborPos, Direction dir, Storage<FluidVariant> storage) {
        Endpoint {
            Objects.requireNonNull(pipePos, "pipePos");
            Objects.requireNonNull(neighborPos, "neighborPos");
            Objects.requireNonNull(dir, "dir");
            Objects.requireNonNull(storage, "storage");
        }
    }

    private record NetworkScan(
            Set<BlockPos> pipes,
            List<Endpoint> endpoints,
            BlockPos controller,
            long maxRate,
            int maxDistance,
            boolean waterOnly
    ) {
        boolean allows(FluidVariant variant) {
            if (!waterOnly) {
                return true;
            }
            return variant.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                    || variant.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER;
        }
    }
}
