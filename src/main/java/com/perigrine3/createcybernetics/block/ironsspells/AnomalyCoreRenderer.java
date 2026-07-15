package com.perigrine3.createcybernetics.block.ironsspells;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AnomalyCoreRenderer implements BlockEntityRenderer<AnomalyCoreBlockEntity> {
    private static final float FULL_ROTATION_TICKS = 160.0F;
    private static final float DEGREES_PER_TICK = 360.0F / FULL_ROTATION_TICKS;

    public AnomalyCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AnomalyCoreBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();

        if (!(state.getBlock() instanceof AnomalyCoreBlock)) {
            return;
        }

        float ticks = (level.getGameTime() % (long) FULL_ROTATION_TICKS) + partialTick;
        float degrees = ticks * DEGREES_PER_TICK;

        BlockState renderState = state.setValue(AnomalyCoreBlock.RENDERING, true);

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                renderState,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }
}