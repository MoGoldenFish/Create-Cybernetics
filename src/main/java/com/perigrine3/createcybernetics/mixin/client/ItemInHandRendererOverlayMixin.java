package com.perigrine3.createcybernetics.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.compat.bettercombat.BetterCombatFirstPersonCompat;
import com.perigrine3.createcybernetics.compat.playeranimator.PlayerAnimatorFirstPersonOverlayCompat;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$hideFirstPersonArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equippedProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        if (!(minecraft.player instanceof AbstractClientPlayer player)) {
            return;
        }

        if (minecraft.getCameraEntity() != player) {
            return;
        }

        if (!createcybernetics$hasArm(player, arm)) {
            ci.cancel();
            return;
        }

        if (BetterCombatFirstPersonCompat.shouldHideFirstPersonArms(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerArm", at = @At("TAIL"))
    private void createcybernetics$renderFirstPersonArmOverlays(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equippedProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        if (!(minecraft.player instanceof AbstractClientPlayer player)) {
            return;
        }

        if (minecraft.getCameraEntity() != player) {
            return;
        }

        if (!createcybernetics$hasArm(player, arm)) {
            return;
        }

        if (!BetterCombatFirstPersonCompat.shouldRenderCreateCyberneticsFirstPersonArms(player)) {
            return;
        }

        if (player.isInvisible()) {
            return;
        }

        EntityRenderer<? super AbstractClientPlayer> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);

        if (!(entityRenderer instanceof PlayerRenderer renderer)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        PlayerAnimatorFirstPersonOverlayCompat.renderVanillaFirstPersonArmOverlays(player, arm, model, poseStack, buffer, packedLight);
    }

    private static boolean createcybernetics$hasArm(AbstractClientPlayer player, HumanoidArm arm) {
        PlayerCyberwareData data =
                player.getData(ModAttachments.CYBERWARE);

        if (arm == HumanoidArm.LEFT) {
            return data.hasAnyTagged(
                    ModTags.Items.LEFTARM_ITEMS,
                    CyberwareSlot.LARM
            );
        }

        return data.hasAnyTagged(
                ModTags.Items.RIGHTARM_ITEMS,
                CyberwareSlot.RARM
        );
    }
}