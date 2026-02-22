package com.billtech.block.entity;

import com.billtech.block.ModBlockEntities;
import com.billtech.menu.ControlConductorPadMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class ControlConductorPadBlockEntity extends BlockEntity implements MenuProvider, SideConfigAccess, MachineStatusAccess, RemoteControllable {
    private static final int PROCESS_TICKS = 120;
    private UUID capturedMob;
    private int chargedTicks;
    private int processTicks;
    private boolean remoteEnabled = true;
    private final SideConfig sideConfig = new SideConfig(PortMode.NONE);

    public ControlConductorPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_CONDUCTOR_PAD, pos, state);
        for (Direction dir : Direction.values()) {
            sideConfig.set(com.billtech.transport.TransportType.ENERGY, dir, PortMode.NONE);
            sideConfig.set(com.billtech.transport.TransportType.ITEM, dir, PortMode.NONE);
            sideConfig.set(com.billtech.transport.TransportType.FLUID, dir, PortMode.NONE);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ControlConductorPadBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel level) {
        if (!remoteEnabled) {
            processTicks = 0;
            return;
        }
        if (chargedTicks > 0) {
            chargedTicks--;
        }
        captureIfNeeded(level);
        Monster mob = getCapturedMob(level);
        if (mob == null) {
            processTicks = 0;
            return;
        }
        immobilize(mob);
        if (isStruckByNaturalLightning(level)) {
            applySyntheticZap(100);
        }
        if (chargedTicks > 0) {
            processTicks++;
            if (processTicks >= PROCESS_TICKS) {
                mob.hurt(level.damageSources().magic(), Float.MAX_VALUE);
                capturedMob = null;
                processTicks = 0;
                setChanged();
            }
        } else {
            processTicks = 0;
        }
    }

    private void captureIfNeeded(ServerLevel level) {
        if (capturedMob != null && getCapturedMob(level) != null) {
            return;
        }
        capturedMob = null;
        AABB area = chamberArea();
        List<Monster> mobs = level.getEntitiesOfClass(Monster.class, area, Entity::isAlive);
        if (mobs.isEmpty()) {
            return;
        }
        capturedMob = mobs.get(0).getUUID();
        setChanged();
    }

    private void immobilize(Monster mob) {
        Vec3 target = Vec3.atCenterOf(worldPosition).add(0.0, 1.0, 0.0);
        mob.setDeltaMovement(0.0, 0.0, 0.0);
        mob.teleportTo(target.x, target.y, target.z);
    }

    private boolean isStruckByNaturalLightning(ServerLevel level) {
        return !level.getEntitiesOfClass(LightningBolt.class, chamberArea().inflate(1.0)).isEmpty();
    }

    public void applySyntheticZap(int ticks) {
        chargedTicks = Math.max(chargedTicks, ticks);
        setChanged();
    }

    public void applyTeslaZap(Level level, int ticks) {
        applySyntheticZap(ticks);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Monster mob = getCapturedMob(serverLevel);
        if (mob == null) {
            return;
        }
        mob.hurt(level.damageSources().lightningBolt(), 2.0f);
        LightningBolt bolt = new LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(mob.getX(), mob.getY(), mob.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    public boolean hasCapturedMob() {
        return capturedMob != null;
    }

    public boolean isCharged() {
        return chargedTicks > 0;
    }

    public Monster getCapturedMob(ServerLevel level) {
        if (capturedMob == null) {
            return null;
        }
        Entity entity = level.getEntity(capturedMob);
        if (entity instanceof Monster monster && entity.isAlive() && chamberArea().inflate(0.5).contains(entity.position())) {
            return monster;
        }
        capturedMob = null;
        return null;
    }

    private AABB chamberArea() {
        return new AABB(
                worldPosition.getX() + 0.15,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.15,
                worldPosition.getX() + 0.85,
                worldPosition.getY() + 2.0,
                worldPosition.getZ() + 0.85
        );
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return Direction.NORTH;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.billtech.control_conductor_pad");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ControlConductorPadMenu(id, inventory, this);
    }

    @Override
    public int getEnergyStored() {
        return chargedTicks;
    }

    @Override
    public int getEnergyCapacity() {
        return 100;
    }

    @Override
    public int getFluidInStored() {
        return hasCapturedMob() ? 1 : 0;
    }

    @Override
    public int getFluidInCapacity() {
        return 1;
    }

    @Override
    public int getFluidOutStored() {
        return processTicks;
    }

    @Override
    public int getFluidOutCapacity() {
        return PROCESS_TICKS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (capturedMob != null) {
            tag.putString("CapturedMob", capturedMob.toString());
        }
        tag.putInt("ChargedTicks", chargedTicks);
        tag.putInt("ProcessTicks", processTicks);
        tag.putBoolean("RemoteEnabled", remoteEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        String uuid = tag.getString("CapturedMob").orElse("");
        capturedMob = uuid.isEmpty() ? null : UUID.fromString(uuid);
        chargedTicks = tag.getInt("ChargedTicks").orElse(0);
        processTicks = tag.getInt("ProcessTicks").orElse(0);
        remoteEnabled = tag.getBoolean("RemoteEnabled").orElse(true);
    }

    @Override
    public MachineRuntimeState getRuntimeState() {
        if (!remoteEnabled) {
            return MachineRuntimeState.DISABLED;
        }
        if (!hasCapturedMob()) {
            return MachineRuntimeState.NO_WORK;
        }
        if (processTicks > 0) {
            return MachineRuntimeState.RUNNING;
        }
        return isCharged() ? MachineRuntimeState.IDLE : MachineRuntimeState.NO_POWER;
    }

    @Override
    public int getProcessProgress() {
        return processTicks;
    }

    @Override
    public int getProcessMax() {
        return PROCESS_TICKS;
    }

    @Override
    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }

    @Override
    public void setRemoteEnabled(boolean enabled) {
        if (remoteEnabled == enabled) {
            return;
        }
        remoteEnabled = enabled;
        if (!enabled) {
            processTicks = 0;
        }
        setChanged();
    }
}
