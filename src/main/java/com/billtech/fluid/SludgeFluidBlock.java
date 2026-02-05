package com.billtech.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.InsideBlockEffectApplier;

public class SludgeFluidBlock extends net.minecraft.world.level.block.LiquidBlock {
    public SludgeFluidBlock(net.minecraft.world.level.material.FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return true;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier) {
        if (entity instanceof LivingEntity living) {
            if (entity instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
                super.entityInside(state, level, pos, entity, applier);
                return;
            }
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), living);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), living);
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.5, motion.y * 0.5, motion.z * 0.5);
        super.entityInside(state, level, pos, entity, applier);
    }
}
