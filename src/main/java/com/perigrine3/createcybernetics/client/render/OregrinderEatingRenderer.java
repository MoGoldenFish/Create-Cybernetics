package com.perigrine3.createcybernetics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.cyberware.organs.OregrinderItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class OregrinderEatingRenderer {

    private static final float VANILLA_EAT_DURATION = 32.0F;

    private OregrinderEatingRenderer() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        if (!OregrinderItem.isEatingMetalForRender(player)) {
            return;
        }

        if (event.getHand() != OregrinderItem.metalEatingHandForRender(player)) {
            return;
        }

        if (!OregrinderItem.isOregrinderMaterialForRender(event.getItemStack())) {
            return;
        }

        HumanoidArm renderedArm = getRenderedArm(player, event.getHand());
        int direction = renderedArm == HumanoidArm.RIGHT ? 1 : -1;

        float useTicks = OregrinderItem.metalEatingTicksForRender(player)
                + event.getPartialTick();

        float eatingCycle = useTicks % VANILLA_EAT_DURATION;
        float progress = eatingCycle / VANILLA_EAT_DURATION;

        applyVanillaEatTransform(
                event.getPoseStack(),
                eatingCycle,
                progress,
                direction
        );
    }

    private static HumanoidArm getRenderedArm(
            LocalPlayer player,
            InteractionHand hand
    ) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getMainArm();
        }

        return player.getMainArm() == HumanoidArm.RIGHT
                ? HumanoidArm.LEFT
                : HumanoidArm.RIGHT;
    }

    private static void applyVanillaEatTransform(
            PoseStack poseStack,
            float useTicks,
            float progress,
            int direction
    ) {
        if (progress < 0.8F) {
            float chewBob = Mth.abs(
                    Mth.cos(useTicks / 4.0F * (float) Math.PI) * 0.1F
            );

            poseStack.translate(0.0F, chewBob, 0.0F);
        }

        float raiseAmount = 1.0F - (float) Math.pow(progress, 27.0F);

        poseStack.translate(
                raiseAmount * 0.6F * direction,
                raiseAmount * -0.5F,
                0.0F
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(direction * raiseAmount * 90.0F)
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(raiseAmount * 10.0F)
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(direction * raiseAmount * 30.0F)
        );
    }
}