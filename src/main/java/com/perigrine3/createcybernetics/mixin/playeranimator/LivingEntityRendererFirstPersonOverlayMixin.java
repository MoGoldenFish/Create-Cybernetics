package com.perigrine3.createcybernetics.mixin.playeranimator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.compat.bettercombat.BetterCombatFirstPersonCompat;
import com.perigrine3.createcybernetics.compat.playeranimator.PlayerAnimatorFirstPersonOverlayCompat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererFirstPersonOverlayMixin {

    @Unique
    private boolean createcybernetics$storedRightArmVisible;

    @Unique
    private boolean createcybernetics$storedRightSleeveVisible;

    @Unique
    private boolean createcybernetics$storedLeftArmVisible;

    @Unique
    private boolean createcybernetics$storedLeftSleeveVisible;

    @Unique
    private boolean createcybernetics$storedArmVisibility;

    @Unique
    private boolean createcybernetics$storedRightLegVisible;

    @Unique
    private boolean createcybernetics$storedRightPantsVisible;

    @Unique
    private boolean createcybernetics$storedLeftLegVisible;

    @Unique
    private boolean createcybernetics$storedLeftPantsVisible;

    @Unique
    private boolean createcybernetics$storedLegVisibility;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void createcybernetics$hideFirstPersonPlayerModelPartsBeforeRender(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        createcybernetics$storedArmVisibility = false;
        createcybernetics$storedLegVisibility = false;

        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = createcybernetics$getPlayerModel();
        if (model == null) {
            return;
        }

        if (PlayerAnimatorFirstPersonOverlayCompat.shouldRenderFirstPersonPlayerOverlay(player)) {
            createcybernetics$storedRightLegVisible = model.rightLeg.visible;
            createcybernetics$storedRightPantsVisible = model.rightPants.visible;
            createcybernetics$storedLeftLegVisible = model.leftLeg.visible;
            createcybernetics$storedLeftPantsVisible = model.leftPants.visible;
            createcybernetics$storedLegVisibility = true;

            model.rightLeg.visible = false;
            model.rightPants.visible = false;
            model.leftLeg.visible = false;
            model.leftPants.visible = false;
        }

        if (!BetterCombatFirstPersonCompat.shouldHideFirstPersonPlayerModelArms(player)) {
            return;
        }

        createcybernetics$storedRightArmVisible = model.rightArm.visible;
        createcybernetics$storedRightSleeveVisible = model.rightSleeve.visible;
        createcybernetics$storedLeftArmVisible = model.leftArm.visible;
        createcybernetics$storedLeftSleeveVisible = model.leftSleeve.visible;
        createcybernetics$storedArmVisibility = true;

        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftArm.visible = false;
        model.leftSleeve.visible = false;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void createcybernetics$restoreFirstPersonPlayerModelPartsAfterRender(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        PlayerModel<AbstractClientPlayer> model = createcybernetics$getPlayerModel();

        if (model == null) {
            createcybernetics$storedArmVisibility = false;
            createcybernetics$storedLegVisibility = false;
            return;
        }

        if (createcybernetics$storedArmVisibility) {
            model.rightArm.visible = createcybernetics$storedRightArmVisible;
            model.rightSleeve.visible = createcybernetics$storedRightSleeveVisible;
            model.leftArm.visible = createcybernetics$storedLeftArmVisible;
            model.leftSleeve.visible = createcybernetics$storedLeftSleeveVisible;

            createcybernetics$storedArmVisibility = false;
        }

        if (createcybernetics$storedLegVisibility) {
            model.rightLeg.visible = createcybernetics$storedRightLegVisible;
            model.rightPants.visible = createcybernetics$storedRightPantsVisible;
            model.leftLeg.visible = createcybernetics$storedLeftLegVisible;
            model.leftPants.visible = createcybernetics$storedLeftPantsVisible;

            createcybernetics$storedLegVisibility = false;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void createcybernetics$renderPlayerAnimatorFirstPersonOverlays(
            LivingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        if (!BetterCombatFirstPersonCompat.shouldRenderCreateCyberneticsFirstPersonArms(player)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = createcybernetics$getPlayerModel();
        if (model == null) return;

        PlayerAnimatorFirstPersonOverlayCompat.renderFirstPersonPlayerOverlays(
                player,
                model,
                poseStack,
                buffer,
                packedLight
        );
    }

    @SuppressWarnings("unchecked")
    @Unique
    private PlayerModel<AbstractClientPlayer> createcybernetics$getPlayerModel() {
        Object self = this;

        if (!(self instanceof LivingEntityRenderer<?, ?> renderer)) {
            return null;
        }

        EntityModel<?> model = renderer.getModel();
        if (!(model instanceof PlayerModel<?> playerModel)) {
            return null;
        }

        return (PlayerModel<AbstractClientPlayer>) playerModel;
    }
}