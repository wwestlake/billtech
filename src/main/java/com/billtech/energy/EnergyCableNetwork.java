package com.billtech.energy;

import com.billtech.block.EnergyCableBlock;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class EnergyCableNetwork {
    private EnergyCableNetwork() {
    }

    public static void tick(Level level, BlockPos origin) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || !scan.controller.equals(origin)) {
            return;
        }
        if (scan.endpoints.isEmpty()) {
            return;
        }
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : scan.endpoints) {
            if (endpoint.storage.supportsExtraction() && endpoint.storage.getAmount() > 0) {
                sources.add(endpoint);
            }
            if (endpoint.storage.supportsInsertion()) {
                sinks.add(endpoint);
            }
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return;
        }
        for (Endpoint source : sources) {
            Map<BlockPos, Integer> distances = bfsDistances(scan.pipes, source.pipePos, scan.maxDistance);
            if (distances.isEmpty()) {
                continue;
            }
            for (Endpoint sink : sinks) {
                if (sink == source) {
                    continue;
                }
                Integer dist = distances.get(sink.pipePos);
                if (dist == null || dist > scan.maxDistance) {
                    continue;
                }
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = EnergyStorageUtil.move(source.storage, sink.storage, scan.maxRate, tx);
                    if (moved > 0) {
                        tx.commit();
                        return;
                    }
                }
            }
        }
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
        if (!(originState.getBlock() instanceof EnergyCableBlock)) {
            return null;
        }
        Set<BlockPos> pipes = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        pipes.add(origin);
        long minRate = EnergyCableTiers.maxRate(originState);
        int minDistance = EnergyCableTiers.maxDistance(originState);
        BlockPos controller = origin;
        long controllerKey = origin.asLong();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            minRate = Math.min(minRate, EnergyCableTiers.maxRate(state));
            minDistance = Math.min(minDistance, EnergyCableTiers.maxDistance(state));
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
                if (nextState.getBlock() instanceof EnergyCableBlock) {
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
                EnergyStorage storage = EnergyStorage.SIDED.find(level, neighbor, dir.getOpposite());
                if (storage == null) {
                    continue;
                }
                endpoints.add(new Endpoint(pipePos, neighbor, storage));
            }
        }
        return new NetworkScan(pipes, endpoints, controller, minRate, minDistance);
    }

    private record Endpoint(BlockPos pipePos, BlockPos neighborPos, EnergyStorage storage) {
    }

    private record NetworkScan(
            Set<BlockPos> pipes,
            List<Endpoint> endpoints,
            BlockPos controller,
            long maxRate,
            int maxDistance
    ) {
    }
}
