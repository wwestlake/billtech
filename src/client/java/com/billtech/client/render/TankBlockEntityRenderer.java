package com.billtech.client.render;

import com.billtech.block.entity.TankBlockEntity;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;

public class TankBlockEntityRenderer implements BlockEntityRenderer<TankBlockEntity> {
    public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            TankBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPos
    ) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        FluidVariant fluidVariant = be.getFluid();
        if (fluidVariant == null || fluidVariant.isBlank()) {
            return;
        }
        long amount = be.getAmount();
        if (amount <= 0) {
            return;
        }
        float fill = Math.min(1.0f, (float) amount / (float) TankBlockEntity.CAPACITY);
        if (fill <= 0.0f) {
            return;
        }

        Fluid fluid = fluidVariant.getFluid();
        FluidState fluidState = fluid.defaultFluidState();
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) {
            return;
        }
        TextureAtlasSprite[] sprites = handler.getFluidSprites(level, be.getBlockPos(), fluidState);
        if (sprites == null || sprites.length == 0 || sprites[0] == null) {
            return;
        }
        TextureAtlasSprite sprite = sprites[0];
        int color = handler.getFluidColor(level, be.getBlockPos(), fluidState);
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        // Render a scaled fluid cuboid inside the tank.
        poseStack.pushPose();
        float inset = 2.0f / 16.0f;
        float x0 = inset;
        float x1 = 1.0f - inset;
        float z0 = inset;
        float z1 = 1.0f - inset;
        float y0 = inset;
        float y1 = inset + fill * (1.0f - inset * 2.0f);
        if (y1 <= y0) {
            poseStack.popPose();
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        drawFluidCuboid(consumer, poseStack, x0, x1, y0, y1, z0, z1, sprite, red, green, blue, packedLight, fill);
        poseStack.popPose();
    }

    private static void drawFluidCuboid(
            VertexConsumer consumer,
            PoseStack poseStack,
            float x0,
            float x1,
            float y0,
            float y1,
            float z0,
            float z1,
            TextureAtlasSprite sprite,
            float red,
            float green,
            float blue,
            int packedLight,
            float fill
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float vFill = v0 + (v1 - v0) * fill;

        // Top
        vertex(consumer, matrix, pose, x0, y1, z0, u0, v0, red, green, blue, packedLight, 0, 1, 0);
        vertex(consumer, matrix, pose, x0, y1, z1, u0, v1, red, green, blue, packedLight, 0, 1, 0);
        vertex(consumer, matrix, pose, x1, y1, z1, u1, v1, red, green, blue, packedLight, 0, 1, 0);
        vertex(consumer, matrix, pose, x1, y1, z0, u1, v0, red, green, blue, packedLight, 0, 1, 0);

        // Bottom
        vertex(consumer, matrix, pose, x0, y0, z0, u0, v0, red, green, blue, packedLight, 0, -1, 0);
        vertex(consumer, matrix, pose, x1, y0, z0, u1, v0, red, green, blue, packedLight, 0, -1, 0);
        vertex(consumer, matrix, pose, x1, y0, z1, u1, v1, red, green, blue, packedLight, 0, -1, 0);
        vertex(consumer, matrix, pose, x0, y0, z1, u0, v1, red, green, blue, packedLight, 0, -1, 0);

        // North
        vertex(consumer, matrix, pose, x0, y0, z0, u0, v0, red, green, blue, packedLight, 0, 0, -1);
        vertex(consumer, matrix, pose, x0, y1, z0, u0, vFill, red, green, blue, packedLight, 0, 0, -1);
        vertex(consumer, matrix, pose, x1, y1, z0, u1, vFill, red, green, blue, packedLight, 0, 0, -1);
        vertex(consumer, matrix, pose, x1, y0, z0, u1, v0, red, green, blue, packedLight, 0, 0, -1);

        // South
        vertex(consumer, matrix, pose, x0, y0, z1, u0, v0, red, green, blue, packedLight, 0, 0, 1);
        vertex(consumer, matrix, pose, x1, y0, z1, u1, v0, red, green, blue, packedLight, 0, 0, 1);
        vertex(consumer, matrix, pose, x1, y1, z1, u1, vFill, red, green, blue, packedLight, 0, 0, 1);
        vertex(consumer, matrix, pose, x0, y1, z1, u0, vFill, red, green, blue, packedLight, 0, 0, 1);

        // West
        vertex(consumer, matrix, pose, x0, y0, z0, u0, v0, red, green, blue, packedLight, -1, 0, 0);
        vertex(consumer, matrix, pose, x0, y0, z1, u1, v0, red, green, blue, packedLight, -1, 0, 0);
        vertex(consumer, matrix, pose, x0, y1, z1, u1, vFill, red, green, blue, packedLight, -1, 0, 0);
        vertex(consumer, matrix, pose, x0, y1, z0, u0, vFill, red, green, blue, packedLight, -1, 0, 0);

        // East
        vertex(consumer, matrix, pose, x1, y0, z0, u0, v0, red, green, blue, packedLight, 1, 0, 0);
        vertex(consumer, matrix, pose, x1, y1, z0, u0, vFill, red, green, blue, packedLight, 1, 0, 0);
        vertex(consumer, matrix, pose, x1, y1, z1, u1, vFill, red, green, blue, packedLight, 1, 0, 0);
        vertex(consumer, matrix, pose, x1, y0, z1, u1, v0, red, green, blue, packedLight, 1, 0, 0);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            int packedLight,
            float nx,
            float ny,
            float nz
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, 1.0f)
                .setUv(u, v)
                .setOverlay(0)
                .setUv2(LightTexture.block(packedLight), LightTexture.sky(packedLight))
                .setNormal(pose, nx, ny, nz);
    }
}
