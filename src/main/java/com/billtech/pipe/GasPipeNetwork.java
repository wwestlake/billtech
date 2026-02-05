package com.billtech.pipe;

import com.billtech.block.CheckValveBlock;
import com.billtech.block.FlowMeterBlock;
import com.billtech.block.GasPipeBlock;
import com.billtech.block.PumpBlock;
import com.billtech.block.RegulatorBlock;
import com.billtech.block.ValveBlock;
import com.billtech.block.entity.FlowMeterBlockEntity;
import com.billtech.block.entity.MethaneTankBlockEntity;
import com.billtech.fluid.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
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

public final class GasPipeNetwork {
    private GasPipeNetwork() {
    }

    public static void tick(Level level, BlockPos origin) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || scan.endpoints.isEmpty()) {
            return;
        }
        if (!scan.pumps.isEmpty()) {
            for (PumpNode pump : scan.pumps) {
                if (tryPumpDirections(
                        scan,
                        pump,
                        pump.facing.getCounterClockWise(),
                        pump.facing.getClockWise()
                )) {
                    return;
                }
            }
        }
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : scan.endpoints) {
            if (endpoint.isTank) {
                sinks.add(endpoint);
            } else {
                sources.add(endpoint);
            }
        }
        if (tryTransferPass(scan, sources, sinks, false)) {
            return;
        }
        // Allow tank-to-machine transfer if nothing else moved.
        List<Endpoint> tankSources = new ArrayList<>();
        List<Endpoint> machineSinks = new ArrayList<>();
        for (Endpoint endpoint : scan.endpoints) {
            if (endpoint.isTank) {
                tankSources.add(endpoint);
            } else {
                machineSinks.add(endpoint);
            }
        }
        tryTransferPass(scan, tankSources, machineSinks, false);
    }

    public static long insertIntoNetwork(
            Level level,
            BlockPos origin,
            FluidVariant variant,
            long maxAmount,
            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || maxAmount <= 0 || variant == null || variant.isBlank()) {
            return 0;
        }
        if (!scan.allows(variant)) {
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

    public static net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount<FluidVariant> peekExtractable(
            Level level,
            BlockPos origin,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null) {
            return null;
        }
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction tx =
                     net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            for (Endpoint endpoint : scan.endpoints) {
                if (excludePos != null && endpoint.neighborPos.equals(excludePos)) {
                    continue;
                }
                var found = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
                        .findExtractableContent(endpoint.storage, tx);
                if (found == null || found.resource() == null || found.resource().isBlank()) {
                    continue;
                }
                if (!scan.allows(found.resource())) {
                    continue;
                }
                return found;
            }
        }
        return null;
    }

    public static long extractFromNetwork(
            Level level,
            BlockPos origin,
            FluidVariant variant,
            long maxAmount,
            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction,
            BlockPos excludePos
    ) {
        NetworkScan scan = scanNetwork(level, origin);
        if (scan == null || maxAmount <= 0 || variant == null) {
            return 0;
        }
        long remaining = maxAmount;
        if (!scan.allows(variant)) {
            return 0;
        }
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
        return maxAmount - remaining;
    }

    private static boolean tryTransferPass(
            NetworkScan scan,
            List<Endpoint> sources,
            List<Endpoint> sinks,
            boolean allowTankToTank
    ) {
        if (sources.isEmpty() || sinks.isEmpty()) {
            return false;
        }
        for (Endpoint source : sources) {
            SourceInfo sourceInfo = findSourceFluid(source);
            if (sourceInfo == null) {
                continue;
            }
            if (!scan.allows(sourceInfo.variant)) {
                continue;
            }
            Map<BlockPos, Integer> distances = bfsDistances(scan.level, scan.pipes, source.pipePos, scan.maxDistance);
            if (distances.isEmpty()) {
                continue;
            }
            for (Endpoint sink : sinks) {
                if (sink == source) {
                    continue;
                }
                if (!allowTankToTank && source.isTank && sink.isTank) {
                    continue;
                }
                Integer dist = distances.get(sink.pipePos);
                if (dist == null || dist > scan.maxDistance) {
                    continue;
                }
                long transferred = tryTransfer(source, sink, sourceInfo.variant, scan.maxRate);
                if (transferred > 0) {
                    recordFlow(scan, transferred);
                    return true;
                }
            }
        }
        return false;
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

    private static Map<BlockPos, Integer> bfsDistances(Level level, Set<BlockPos> pipes, BlockPos start, int maxDistance) {
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
                if (!canTraverse(level, current, next, dir)) {
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

    private static boolean canTraverse(Level level, BlockPos from, BlockPos to, Direction dir) {
        BlockState fromState = level.getBlockState(from);
        BlockState toState = level.getBlockState(to);
        if (!isPipeLike(fromState) || !isPipeLike(toState)) {
            return false;
        }
        if (fromState.getBlock() instanceof CheckValveBlock) {
            Direction facing = fromState.getValue(CheckValveBlock.FACING);
            Direction output = facing.getClockWise();
            if (dir != output) {
                return false;
            }
        }
        if (toState.getBlock() instanceof CheckValveBlock) {
            Direction facing = toState.getValue(CheckValveBlock.FACING);
            Direction input = facing.getCounterClockWise();
            if (dir != input) {
                return false;
            }
        }
        if (fromState.getBlock() instanceof ValveBlock && !fromState.getValue(ValveBlock.OPEN)) {
            return false;
        }
        if (toState.getBlock() instanceof ValveBlock && !toState.getValue(ValveBlock.OPEN)) {
            return false;
        }
        if (fromState.getBlock() instanceof RegulatorBlock && !fromState.getValue(RegulatorBlock.OPEN)) {
            return false;
        }
        return !(toState.getBlock() instanceof RegulatorBlock) || toState.getValue(RegulatorBlock.OPEN);
    }

    private static boolean isPipeLike(BlockState state) {
        return state.getBlock() instanceof GasPipeBlock
                || state.getBlock() instanceof PumpBlock
                || state.getBlock() instanceof ValveBlock
                || state.getBlock() instanceof CheckValveBlock
                || state.getBlock() instanceof FlowMeterBlock
                || state.getBlock() instanceof RegulatorBlock;
    }

    private static NetworkScan scanNetwork(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!isPipeLike(originState)) {
            return null;
        }
        Set<BlockPos> pipes = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        pipes.add(origin);
        long minRate = GasPipeTiers.maxRate(originState);
        int minDistance = GasPipeTiers.maxDistance(originState);
        List<PumpNode> pumps = new ArrayList<>();
        List<BlockPos> meters = new ArrayList<>();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            minRate = Math.min(minRate, GasPipeTiers.maxRate(state));
            minDistance = Math.min(minDistance, GasPipeTiers.maxDistance(state));
            if (state.getBlock() instanceof PumpBlock) {
                Direction facing = state.getValue(PumpBlock.FACING);
                pumps.add(new PumpNode(pos, facing));
            }
            if (state.getBlock() instanceof FlowMeterBlock) {
                meters.add(pos);
            }
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
        List<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos pipePos : pipes) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pipePos.relative(dir);
                if (pipes.contains(neighbor)) {
                    continue;
                }
                Storage<FluidVariant> storage;
                boolean isTank = false;
                if (level.getBlockEntity(neighbor) instanceof MethaneTankBlockEntity tank) {
                    storage = tank.getStorage();
                    isTank = true;
                } else {
                    storage = FluidStorage.SIDED.find(level, neighbor, dir.getOpposite());
                }
                if (storage == null || !acceptsMethane(storage)) {
                    continue;
                }
                endpoints.add(new Endpoint(pipePos, neighbor, dir, storage, isTank));
            }
        }
        return new NetworkScan(level, pipes, endpoints, pumps, meters, minRate, minDistance);
    }

    private record SourceInfo(FluidVariant variant) {
    }

    private record Endpoint(BlockPos pipePos, BlockPos neighborPos, Direction dir, Storage<FluidVariant> storage, boolean isTank) {
        Endpoint {
            Objects.requireNonNull(pipePos, "pipePos");
            Objects.requireNonNull(neighborPos, "neighborPos");
            Objects.requireNonNull(dir, "dir");
            Objects.requireNonNull(storage, "storage");
        }
    }

    private static boolean tryPumpDirections(
            NetworkScan scan,
            PumpNode pump,
            Direction inDir,
            Direction outDir
    ) {
        BlockPos inStart = pump.pos.relative(inDir);
        BlockPos outStart = pump.pos.relative(outDir);
        Set<BlockPos> inSide = bfsSide(scan.level, scan.pipes, inStart, pump.pos, scan.maxDistance);
        Set<BlockPos> outSide = bfsSide(scan.level, scan.pipes, outStart, pump.pos, scan.maxDistance);
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : scan.endpoints) {
            if (endpoint.pipePos.equals(pump.pos)) {
                if (endpoint.dir == inDir) {
                    sources.add(endpoint);
                } else if (endpoint.dir == outDir) {
                    sinks.add(endpoint);
                }
            } else if (inSide.contains(endpoint.pipePos)) {
                sources.add(endpoint);
            } else if (outSide.contains(endpoint.pipePos)) {
                sinks.add(endpoint);
            }
        }
        return tryTransferPass(scan, sources, sinks, false);
    }

    private static Set<BlockPos> bfsSide(Level level, Set<BlockPos> pipes, BlockPos start, BlockPos pumpPos, int maxDistance) {
        if (!pipes.contains(start)) {
            return java.util.Collections.emptySet();
        }
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> distances = new HashMap<>();
        visited.add(start);
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
                if (next.equals(pumpPos)) {
                    continue;
                }
                if (!pipes.contains(next)) {
                    continue;
                }
                if (!canTraverse(level, current, next, dir)) {
                    continue;
                }
                if (visited.contains(next)) {
                    continue;
                }
                visited.add(next);
                distances.put(next, dist + 1);
                queue.add(next);
            }
        }
        return visited;
    }

    private static void recordFlow(NetworkScan scan, long amount) {
        if (amount <= 0 || scan.meters.isEmpty()) {
            return;
        }
        for (BlockPos pos : scan.meters) {
            if (scan.level.getBlockEntity(pos) instanceof FlowMeterBlockEntity meter) {
                meter.recordMove(amount);
            }
        }
    }

    private record PumpNode(BlockPos pos, Direction facing) {
    }

    private static boolean acceptsMethane(Storage<FluidVariant> storage) {
        for (StorageView<FluidVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) {
                continue;
            }
            return view.getResource().getFluid() == ModFluids.METHANE;
        }
        return true;
    }

    private record NetworkScan(
            Level level,
            Set<BlockPos> pipes,
            List<Endpoint> endpoints,
            List<PumpNode> pumps,
            List<BlockPos> meters,
            long maxRate,
            int maxDistance
    ) {
        boolean allows(FluidVariant variant) {
            return GasPipeTiers.allows(level.getBlockState(pipes.iterator().next()), variant);
        }
    }
}
