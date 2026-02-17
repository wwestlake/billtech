package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SentryContainerBlockEntity extends BlockEntity {
    private UUID guardMob;

    public SentryContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SENTRY_CONTAINER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SentryContainerBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel level) {
        captureIfNeeded(level);
        AbstractSkeleton guard = getGuard(level);
        if (guard == null) {
            return;
        }
        holdInChamber(guard);
    }

    private void captureIfNeeded(ServerLevel level) {
        if (guardMob != null && getGuard(level) != null) {
            return;
        }
        guardMob = null;
        List<AbstractSkeleton> skeletons = level.getEntitiesOfClass(AbstractSkeleton.class, chamberArea(), Entity::isAlive);
        if (skeletons.isEmpty()) {
            return;
        }
        guardMob = skeletons.get(0).getUUID();
        setChanged();
    }

    public void maintainGuard(ServerLevel level, boolean active, UUID owner) {
        captureIfNeeded(level);
        AbstractSkeleton guard = getGuard(level);
        if (guard == null) {
            return;
        }
        holdInChamber(guard);
        if (!active) {
            return;
        }
        guard.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 8, true, false, true));
        guard.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
        Player target = findUnauthorizedTarget(level, owner);
        guard.setTarget(target);
    }

    private Player findUnauthorizedTarget(ServerLevel level, UUID owner) {
        AABB scan = chamberArea().inflate(10.0, 3.0, 10.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, scan,
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative() && !player.getUUID().equals(owner));
        return players.stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(Vec3.atCenterOf(worldPosition))))
                .orElse(null);
    }

    private void holdInChamber(AbstractSkeleton skeleton) {
        Vec3 center = Vec3.atCenterOf(worldPosition).add(0.0, 0.1, 0.0);
        skeleton.setDeltaMovement(0.0, 0.0, 0.0);
        skeleton.teleportTo(center.x, center.y, center.z);
    }

    private AbstractSkeleton getGuard(ServerLevel level) {
        if (guardMob == null) {
            return null;
        }
        Entity e = level.getEntity(guardMob);
        if (e instanceof AbstractSkeleton skeleton && e.isAlive() && chamberArea().inflate(1.0).contains(e.position())) {
            return skeleton;
        }
        guardMob = null;
        return null;
    }

    private AABB chamberArea() {
        return new AABB(worldPosition).inflate(0.45);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (guardMob != null) {
            tag.putString("GuardMob", guardMob.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        String uuid = tag.getString("GuardMob").orElse("");
        guardMob = uuid.isEmpty() ? null : UUID.fromString(uuid);
    }
}
