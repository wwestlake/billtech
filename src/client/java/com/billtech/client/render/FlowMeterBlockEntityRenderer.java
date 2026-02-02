package com.billtech.client.render;

import com.billtech.block.FlowMeterBlock;
import com.billtech.block.entity.FlowMeterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class FlowMeterBlockEntityRenderer implements BlockEntityRenderer<FlowMeterBlockEntity> {
    private final Font font;

    public FlowMeterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(
            FlowMeterBlockEntity blockEntity,
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
        long rate = blockEntity.getLastMoved();
        String text = Long.toString(rate);
        Direction face = blockEntity.getBlockState().getValue(FlowMeterBlock.DISPLAY);
        boolean reversed = blockEntity.getBlockState().getValue(FlowMeterBlock.REVERSED);
        float scale = 0.006f;

        poseStack.pushPose();
        applyFaceTransform(poseStack, face);
        poseStack.scale(scale, -scale, scale);
        int width = font.width(text);
        font.drawInBatch(text, -width / 2f, -6, 0xE0E0E0, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        // Arrow line + head (simple)
        int arrowColor = 0xFFE0B020;
        int len = 18;
        int y = 8;
        int x0 = -len / 2;
        int x1 = len / 2;
        if (reversed) {
            int tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        drawLine(font, bufferSource, poseStack, x0, y, x1, y, arrowColor, packedLight);
        drawLine(font, bufferSource, poseStack, x1, y, x1 - (reversed ? -4 : 4), y - 3, arrowColor, packedLight);
        drawLine(font, bufferSource, poseStack, x1, y, x1 - (reversed ? -4 : 4), y + 3, arrowColor, packedLight);
        poseStack.popPose();
    }

    private static void applyFaceTransform(PoseStack poseStack, Direction face) {
        float offset = 0.26f;
        poseStack.translate(0.5f, 0.5f, 0.5f);
        switch (face) {
            case NORTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                poseStack.translate(0f, 0f, -offset);
            }
            case SOUTH -> poseStack.translate(0f, 0f, offset);
            case EAST -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
                poseStack.translate(0f, 0f, offset);
            }
            case WEST -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
                poseStack.translate(0f, 0f, offset);
            }
            default -> poseStack.translate(0f, 0f, offset);
        }
    }

    private void drawLine(
            Font font,
            MultiBufferSource bufferSource,
            PoseStack poseStack,
            int x0,
            int y0,
            int x1,
            int y1,
            int color,
            int packedLight
    ) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + dx * i / steps;
            int y = y0 + dy * i / steps;
            font.drawInBatch("·", x, y, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        }
    }
}
