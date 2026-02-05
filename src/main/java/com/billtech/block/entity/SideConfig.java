package com.billtech.block.entity;

import com.billtech.transport.TransportType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import java.util.EnumMap;

public class SideConfig {
    private final EnumMap<TransportType, EnumMap<Direction, PortMode>> config = new EnumMap<>(TransportType.class);

    public SideConfig(PortMode defaultMode) {
        for (TransportType type : TransportType.values()) {
            EnumMap<Direction, PortMode> map = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.values()) {
                map.put(dir, defaultMode);
            }
            config.put(type, map);
        }
    }

    public PortMode get(TransportType type, Direction dir) {
        return config.get(type).get(dir);
    }

    public void set(TransportType type, Direction dir, PortMode mode) {
        config.get(type).put(dir, mode);
    }

    public void cycle(TransportType type, Direction dir) {
        PortMode current = get(type, dir);
        set(type, dir, current.next());
    }

    public boolean allowsInsert(TransportType type, Direction dir) {
        return get(type, dir).allowsInsert();
    }

    public boolean allowsExtract(TransportType type, Direction dir) {
        return get(type, dir).allowsExtract();
    }

    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        for (TransportType type : TransportType.values()) {
            CompoundTag typeTag = new CompoundTag();
            for (Direction dir : Direction.values()) {
                typeTag.putInt(dir.getName(), get(type, dir).ordinal());
            }
            root.put(type.name(), typeTag);
        }
        tag.put("SideConfig", root);
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        if (!tag.contains("SideConfig")) {
            return;
        }
        CompoundTag root = tag.getCompound("SideConfig").orElse(null);
        if (root == null) {
            return;
        }
        for (TransportType type : TransportType.values()) {
            if (!root.contains(type.name())) {
                continue;
            }
            CompoundTag typeTag = root.getCompound(type.name()).orElse(null);
            if (typeTag == null) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                int idx = typeTag.getInt(dir.getName()).orElse(PortMode.NONE.ordinal());
                PortMode mode = PortMode.values()[Math.max(0, Math.min(PortMode.values().length - 1, idx))];
                set(type, dir, mode);
            }
        }
    }
}
