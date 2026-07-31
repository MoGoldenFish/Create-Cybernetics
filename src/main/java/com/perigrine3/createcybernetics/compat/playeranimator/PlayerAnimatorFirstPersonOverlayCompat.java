package com.perigrine3.createcybernetics.compat.playeranimator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.client.skin.SkinHighlight;
import com.perigrine3.createcybernetics.client.skin.SkinModifier;
import com.perigrine3.createcybernetics.client.skin.SkinModifierManager;
import com.perigrine3.createcybernetics.client.skin.SkinModifierState;
import com.perigrine3.createcybernetics.client.skin.SkinRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PlayerAnimatorFirstPersonOverlayCompat {

    private static final String FIRST_PERSON_MODE_CLASS =
            "dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode";

    private static Method isFirstPersonPassMethod;
    private static boolean reflectionFailed;

    private PlayerAnimatorFirstPersonOverlayCompat() {}

    public static boolean shouldRenderFirstPersonPlayerOverlay(AbstractClientPlayer player) {
        if (player == null) return false;
        if (Minecraft.getInstance().getCameraEntity() != player) return false;
        return isFirstPersonPass();
    }

    public static void renderFirstPersonPlayerOverlays(
            AbstractClientPlayer player,
            PlayerModel<AbstractClientPlayer> model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (player == null || model == null || poseStack == null || buffer == null) return;
        if (!shouldRenderFirstPersonPlayerOverlay(player)) return;

        SkinModifierState state = SkinModifierManager.getPlayerSkinState(player);
        if (state == null) return;

        List<SkinModifier> modifiers = new ArrayList<>(state.getModifiers());
        List<SkinHighlight> highlights = new ArrayList<>(state.getHighlights());

        if (modifiers.isEmpty() && highlights.isEmpty()) return;

        PlayerSkin.Model modelType = player.getSkin().model();

        renderAnimatedArmOverlays(
                modifiers,
                highlights,
                modelType,
                model.rightArm,
                model.rightSleeve,
                poseStack,
                buffer,
                packedLight
        );

        renderAnimatedArmOverlays(
                modifiers,
                highlights,
                modelType,
                model.leftArm,
                model.leftSleeve,
                poseStack,
                buffer,
                packedLight
        );
    }

    private static void renderAnimatedArmOverlays(
            List<SkinModifier> modifiers,
            List<SkinHighlight> highlights,
            PlayerSkin.Model modelType,
            ModelPart armPart,
            ModelPart sleevePart,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        boolean renderArm = armPart.visible;
        boolean renderSleeve = sleevePart.visible;

        if (!renderArm && !renderSleeve) return;

        renderArmModifiers(
                modifiers,
                modelType,
                armPart,
                sleevePart,
                renderArm,
                renderSleeve,
                poseStack,
                buffer,
                packedLight
        );

        renderArmHighlights(
                highlights,
                modelType,
                armPart,
                sleevePart,
                renderArm,
                renderSleeve,
                poseStack,
                buffer,
                packedLight
        );
    }

    private static boolean isFirstPersonPass() {
        if (reflectionFailed) return false;

        try {
            if (isFirstPersonPassMethod == null) {
                Class<?> modeClass = Class.forName(
                        FIRST_PERSON_MODE_CLASS,
                        false,
                        PlayerAnimatorFirstPersonOverlayCompat.class.getClassLoader()
                );

                isFirstPersonPassMethod = modeClass.getMethod("isFirstPersonPass");
                isFirstPersonPassMethod.setAccessible(true);
            }

            Object result = isFirstPersonPassMethod.invoke(null);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            reflectionFailed = true;
            return false;
        }
    }

    public static void renderVanillaFirstPersonArmOverlays(
            AbstractClientPlayer player,
            HumanoidArm arm,
            PlayerModel<AbstractClientPlayer> model,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (player == null || arm == null || model == null || poseStack == null || buffer == null) return;
        if (Minecraft.getInstance().getCameraEntity() != player) return;

        SkinModifierState state = SkinModifierManager.getPlayerSkinState(player);
        if (state == null) return;

        List<SkinModifier> modifiers = new ArrayList<>(state.getModifiers());
        List<SkinHighlight> highlights = new ArrayList<>(state.getHighlights());

        if (modifiers.isEmpty() && highlights.isEmpty()) return;

        PlayerSkin.Model modelType = player.getSkin().model();

        ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart sleevePart = arm == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;

        renderArmModifiers(
                modifiers,
                modelType,
                armPart,
                sleevePart,
                true,
                true,
                poseStack,
                buffer,
                packedLight
        );

        renderArmHighlights(
                highlights,
                modelType,
                armPart,
                sleevePart,
                true,
                true,
                poseStack,
                buffer,
                packedLight
        );
    }

    private static void renderArmModifiers(
            List<SkinModifier> modifiers,
            PlayerSkin.Model modelType,
            ModelPart armPart,
            ModelPart sleevePart,
            boolean renderArm,
            boolean renderSleeve,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (modifiers.isEmpty()) return;

        for (SkinModifier modifier : modifiers) {
            if (modifier == null) continue;

            ResourceLocation texture = modifier.getTexture(modelType);
            int color = modifier.getColor();

            var vc = buffer.getBuffer(RenderType.entityTranslucent(texture));

            if (renderArm) {
                armPart.render(
                        poseStack,
                        vc,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        color
                );
            }

            if (renderSleeve) {
                sleevePart.render(
                        poseStack,
                        vc,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        color
                );
            }

            if (modifier.hasGlint()) {
                var glintVc = buffer.getBuffer(RenderType.entityGlint());

                if (renderArm) {
                    armPart.render(
                            poseStack,
                            glintVc,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            0xFFFFFFFF
                    );
                }

                if (renderSleeve) {
                    sleevePart.render(
                            poseStack,
                            glintVc,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            0xFFFFFFFF
                    );
                }
            }
        }
    }

    private static void renderArmHighlights(
            List<SkinHighlight> highlights,
            PlayerSkin.Model modelType,
            ModelPart armPart,
            ModelPart sleevePart,
            boolean renderArm,
            boolean renderSleeve,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (highlights.isEmpty()) return;

        for (SkinHighlight highlight : highlights) {
            if (highlight == null) continue;

            ResourceLocation texture = highlight.getTexture(modelType);

            RenderType renderType;
            int light;
            int color;

            if (highlight.isEmissive()) {
                light = 0x00F000F0;

                if (highlight.tintOnEmissive()) {
                    renderType = SkinRenderTypes.emissiveTinted(texture);
                    color = highlight.getColor();
                } else {
                    renderType = RenderType.entityTranslucent(texture);
                    color = 0xFFFFFFFF;
                }
            } else {
                light = packedLight;
                renderType = RenderType.entityTranslucent(texture);
                color = highlight.getColor();
            }

            var vc = buffer.getBuffer(renderType);

            if (renderArm) {
                armPart.render(
                        poseStack,
                        vc,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        color
                );
            }

            if (renderSleeve) {
                sleevePart.render(
                        poseStack,
                        vc,
                        light,
                        OverlayTexture.NO_OVERLAY,
                        color
                );
            }
        }
    }
}