package com.billtech.client.render;

import com.billtech.cover.CoverProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PipeCoverBlockEntityRenderer<T extends BlockEntity & CoverProvider> implements BlockEntityRenderer<T> {
    private static final float THICKNESS = 1.0f / 16.0f;
    private final BlockRenderDispatcher dispatcher;

    public PipeCoverBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            T blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPos
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockState cover = blockEntity.getCoverState(dir);
            if (cover == null) {
                continue;
            }
            poseStack.pushPose();
            applyTransform(poseStack, dir);
            dispatcher.renderSingleBlock(cover, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void applyTransform(PoseStack poseStack, Direction dir) {
        switch (dir) {
            case NORTH -> {
                poseStack.translate(0.0, 0.0, 0.0);
                poseStack.scale(1.0f, 1.0f, THICKNESS);
            }
            case SOUTH -> {
                poseStack.translate(0.0, 0.0, 1.0f - THICKNESS);
                poseStack.scale(1.0f, 1.0f, THICKNESS);
            }
            case WEST -> {
                poseStack.translate(0.0, 0.0, 0.0);
                poseStack.scale(THICKNESS, 1.0f, 1.0f);
            }
            case EAST -> {
                poseStack.translate(1.0f - THICKNESS, 0.0, 0.0);
                poseStack.scale(THICKNESS, 1.0f, 1.0f);
            }
            case DOWN -> {
                poseStack.translate(0.0, 0.0, 0.0);
                poseStack.scale(1.0f, THICKNESS, 1.0f);
            }
            case UP -> {
                poseStack.translate(0.0, 1.0f - THICKNESS, 0.0);
                poseStack.scale(1.0f, THICKNESS, 1.0f);
            }
        }
    }
}
