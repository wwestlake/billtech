package com.billtech.client.render;

import com.billtech.cover.CoverProvider;
import com.billtech.stripe.StripeCarrier;
import com.billtech.stripe.StripeData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class PipeCoverBlockEntityRenderer<T extends BlockEntity & CoverProvider> implements BlockEntityRenderer<T> {
    private static final float THICKNESS = 1.0f / 16.0f;
    private static final float PIPE_MIN = 6f / 16f;
    private static final float PIPE_MAX = 10f / 16f;
    private static final float STRIPE_EPS = 0.0045f;
    private static final float BRIGHTEN = 1.0f;
    private static final float COLOR_FLOOR = 0.0f;
    private static final float STRIPE_WIDTH = 0.035f;
    private static final float STRIPE_GAP = 0.015f;
    private static final ResourceLocation STRIPE_TEXTURE_ID = ResourceLocation.fromNamespaceAndPath("billtech", "textures/block/pipe_stripe.png");
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
        renderStripes(blockEntity, poseStack, bufferSource, packedLight);
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

    private void renderStripes(T be, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (!(be instanceof StripeCarrier carrier)) {
            return;
        }
        StripeData data = carrier.getStripeData();
        if (data == null || data.isEmpty()) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(STRIPE_TEXTURE_ID));
        PoseStack.Pose pose = poseStack.last();
        int[] colors = data.colors();
        int stripeLight = sampleStripeLight(be, packedLight);
        BlockState state = be.getBlockState();
        boolean north = isConnected(state, "north");
        boolean south = isConnected(state, "south");
        boolean east = isConnected(state, "east");
        boolean west = isConnected(state, "west");
        boolean up = isConnected(state, "up");
        boolean down = isConnected(state, "down");

        if (west) {
            drawAxisX(vc, pose, 0f, PIPE_MIN, colors, data.stripeCount(), stripeLight);
        }
        if (east) {
            drawAxisX(vc, pose, PIPE_MAX, 1f, colors, data.stripeCount(), stripeLight);
        }
        if (west && east) {
            drawAxisX(vc, pose, PIPE_MIN, PIPE_MAX, colors, data.stripeCount(), stripeLight);
        }

        if (north) {
            drawAxisZ(vc, pose, 0f, PIPE_MIN, colors, data.stripeCount(), stripeLight);
        }
        if (south) {
            drawAxisZ(vc, pose, PIPE_MAX, 1f, colors, data.stripeCount(), stripeLight);
        }
        if (north && south) {
            drawAxisZ(vc, pose, PIPE_MIN, PIPE_MAX, colors, data.stripeCount(), stripeLight);
        }

        if (down) {
            drawAxisY(vc, pose, 0f, PIPE_MIN, colors, data.stripeCount(), stripeLight);
        }
        if (up) {
            drawAxisY(vc, pose, PIPE_MAX, 1f, colors, data.stripeCount(), stripeLight);
        }
        if (down && up) {
            drawAxisY(vc, pose, PIPE_MIN, PIPE_MAX, colors, data.stripeCount(), stripeLight);
        }

        // Isolated segment: show a short centered stripe marker on the center cube.
        if (!north && !south && !east && !west && !up && !down) {
            drawAxisX(vc, pose, PIPE_MIN, PIPE_MAX, colors, data.stripeCount(), stripeLight);
        }
    }

    private int sampleStripeLight(T be, int fallbackLight) {
        if (be.getLevel() == null) {
            return fallbackLight;
        }
        BlockPos pos = be.getBlockPos();
        int block = LightTexture.block(fallbackLight);
        int sky = LightTexture.sky(fallbackLight);
        for (Direction dir : Direction.values()) {
            int sample = LevelRenderer.getLightColor(be.getLevel(), pos.relative(dir));
            block = Math.max(block, LightTexture.block(sample));
            sky = Math.max(sky, LightTexture.sky(sample));
        }
        return LightTexture.pack(block, sky);
    }

    private void drawAxisX(
            VertexConsumer vc,
            PoseStack.Pose pose,
            float x1,
            float x2,
            int[] colors,
            int count,
            int light
    ) {
        float total = (count * STRIPE_WIDTH) + ((count - 1) * STRIPE_GAP);
        float bandStart = 0.5f - (total * 0.5f);
        for (int i = 0; i < count; i++) {
            float band1 = bandStart + i * (STRIPE_WIDTH + STRIPE_GAP);
            float band2 = band1 + STRIPE_WIDTH;
            float[] rgb = colorOf(colors[i]);
            // side faces only (no X endcaps)
            addQuad(vc, pose, x1, band1, x2, band2, PIPE_MIN - STRIPE_EPS, 0, 0, -1, rgb[0], rgb[1], rgb[2], light);
            addQuad(vc, pose, x1, band1, x2, band2, PIPE_MAX + STRIPE_EPS, 0, 0, 1, rgb[0], rgb[1], rgb[2], light);
            addQuadY(vc, pose, x1, PIPE_MAX + STRIPE_EPS, band1, x2, band2, 0, 1, 0, rgb[0], rgb[1], rgb[2], light);
            addQuadY(vc, pose, x1, PIPE_MIN - STRIPE_EPS, band1, x2, band2, 0, -1, 0, rgb[0], rgb[1], rgb[2], light);
        }
    }

    private void drawAxisZ(
            VertexConsumer vc,
            PoseStack.Pose pose,
            float z1,
            float z2,
            int[] colors,
            int count,
            int light
    ) {
        float total = (count * STRIPE_WIDTH) + ((count - 1) * STRIPE_GAP);
        float bandStart = 0.5f - (total * 0.5f);
        for (int i = 0; i < count; i++) {
            float band1 = bandStart + i * (STRIPE_WIDTH + STRIPE_GAP);
            float band2 = band1 + STRIPE_WIDTH;
            float[] rgb = colorOf(colors[i]);
            // side faces only (no Z endcaps)
            addQuadZ(vc, pose, PIPE_MAX + STRIPE_EPS, band1, z1, band2, z2, 1, 0, 0, rgb[0], rgb[1], rgb[2], light);
            addQuadZ(vc, pose, PIPE_MIN - STRIPE_EPS, band1, z1, band2, z2, -1, 0, 0, rgb[0], rgb[1], rgb[2], light);
            addQuadY(vc, pose, band1, PIPE_MAX + STRIPE_EPS, z1, band2, z2, 0, 1, 0, rgb[0], rgb[1], rgb[2], light);
            addQuadY(vc, pose, band1, PIPE_MIN - STRIPE_EPS, z1, band2, z2, 0, -1, 0, rgb[0], rgb[1], rgb[2], light);
        }
    }

    private void drawAxisY(
            VertexConsumer vc,
            PoseStack.Pose pose,
            float y1,
            float y2,
            int[] colors,
            int count,
            int light
    ) {
        float total = (count * STRIPE_WIDTH) + ((count - 1) * STRIPE_GAP);
        float bandStart = 0.5f - (total * 0.5f);
        for (int i = 0; i < count; i++) {
            float band1 = bandStart + i * (STRIPE_WIDTH + STRIPE_GAP);
            float band2 = band1 + STRIPE_WIDTH;
            float[] rgb = colorOf(colors[i]);
            // side faces only (no Y endcaps)
            addQuad(vc, pose, band1, y1, band2, y2, PIPE_MIN - STRIPE_EPS, 0, 0, -1, rgb[0], rgb[1], rgb[2], light);
            addQuad(vc, pose, band1, y1, band2, y2, PIPE_MAX + STRIPE_EPS, 0, 0, 1, rgb[0], rgb[1], rgb[2], light);
            addQuadZ(vc, pose, PIPE_MAX + STRIPE_EPS, y1, band1, y2, band2, 1, 0, 0, rgb[0], rgb[1], rgb[2], light);
            addQuadZ(vc, pose, PIPE_MIN - STRIPE_EPS, y1, band1, y2, band2, -1, 0, 0, rgb[0], rgb[1], rgb[2], light);
        }
    }

    private float[] colorOf(int dyeId) {
        int color = DyeColor.byId(dyeId).getFireworkColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        r = Math.min(1f, Math.max(COLOR_FLOOR, r * BRIGHTEN));
        g = Math.min(1f, Math.max(COLOR_FLOOR, g * BRIGHTEN));
        b = Math.min(1f, Math.max(COLOR_FLOOR, b * BRIGHTEN));
        return new float[]{r, g, b};
    }

    private boolean isConnected(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null || !state.hasProperty(property)) {
            return false;
        }
        Object value = state.getValue(property);
        return value != null && !"none".equalsIgnoreCase(value.toString());
    }

    private void addQuad(VertexConsumer vc, PoseStack.Pose pose,
                         float x1, float y1, float x2, float y2, float z,
                         float nx, float ny, float nz, float r, float g, float b, int light) {
        float u1 = 0f;
        float u2 = 1f;
        float v1 = 0f;
        float v2 = 1f;
        vc.addVertex(pose.pose(), x1, y1, z)
                .setColor(r, g, b, 1f)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x2, y1, z)
                .setColor(r, g, b, 1f)
                .setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x2, y2, z)
                .setColor(r, g, b, 1f)
                .setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x1, y2, z)
                .setColor(r, g, b, 1f)
                .setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    private void addQuadZ(VertexConsumer vc, PoseStack.Pose pose,
                          float x, float y1, float z1, float y2, float z2,
                          float nx, float ny, float nz, float r, float g, float b, int light) {
        float u1 = 0f;
        float u2 = 1f;
        float v1 = 0f;
        float v2 = 1f;
        vc.addVertex(pose.pose(), x, y1, z1)
                .setColor(r, g, b, 1f)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x, y1, z2)
                .setColor(r, g, b, 1f)
                .setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x, y2, z2)
                .setColor(r, g, b, 1f)
                .setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x, y2, z1)
                .setColor(r, g, b, 1f)
                .setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    private void addQuadY(
            VertexConsumer vc,
            PoseStack.Pose pose,
            float x1, float y, float z1, float x2, float z2,
            float nx, float ny, float nz, float r, float g, float b, int light
    ) {
        float u1 = 0f;
        float u2 = 1f;
        float v1 = 0f;
        float v2 = 1f;
        vc.addVertex(pose.pose(), x1, y, z1)
                .setColor(r, g, b, 1f)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x2, y, z1)
                .setColor(r, g, b, 1f)
                .setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x2, y, z2)
                .setColor(r, g, b, 1f)
                .setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
        vc.addVertex(pose.pose(), x1, y, z2)
                .setColor(r, g, b, 1f)
                .setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
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
