package com.perigrine3.createcybernetics.mixin.fancymenu;

import net.minecraft.client.model.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.model.PlayerEntityModel",
        remap = false
)
public abstract class FancyMenuPlayerEntityModelMixin {

    @Inject(
            method = "setupAnimWithoutEntity(FFFFF)V",
            at = @At("TAIL"),
            remap = false
    )
    private void createCybernetics$showAllLimbs(
            float animationSpeed,
            float animationSpeedOld,
            float ageInTicks,
            float headRotY,
            float headRotX,
            CallbackInfo ci
    ) {
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;

        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;

        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
    }
}