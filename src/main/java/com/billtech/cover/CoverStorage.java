package com.billtech.cover;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CoverStorage {
    private static final String TAG_COVERS = "Covers";
    private final Map<Direction, ResourceLocation> covers = new EnumMap<>(Direction.class);

    public boolean hasCover(Direction side) {
        return covers.containsKey(side);
    }

    public void setCover(Direction side, ResourceLocation blockId) {
        if (blockId == null) {
            covers.remove(side);
            return;
        }
        covers.put(side, blockId);
    }

    public void clearCover(Direction side) {
        covers.remove(side);
    }

    public BlockState getCoverState(Direction side) {
        ResourceLocation id = covers.get(side);
        if (id == null) {
            return null;
        }
        Holder.Reference<Block> holder = BuiltInRegistries.BLOCK.get(id).orElse(null);
        Block block = holder == null ? Blocks.AIR : holder.value();
        if (block == Blocks.AIR) {
            return null;
        }
        return block.defaultBlockState();
    }

    public void load(CompoundTag tag) {
        covers.clear();
        if (!tag.contains(TAG_COVERS)) {
            return;
        }
        CompoundTag coversTag = tag.getCompound(TAG_COVERS).orElse(null);
        if (coversTag == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            String key = dir.getName();
            if (!coversTag.contains(key)) {
                continue;
            }
            coversTag.getString(key).ifPresent(value -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null) {
                    covers.put(dir, id);
                }
            });
        }
    }

    public void save(CompoundTag tag) {
        if (covers.isEmpty()) {
            return;
        }
        CompoundTag coversTag = new CompoundTag();
        for (Map.Entry<Direction, ResourceLocation> entry : covers.entrySet()) {
            coversTag.putString(entry.getKey().getName(), entry.getValue().toString());
        }
        tag.put(TAG_COVERS, coversTag);
    }
}
