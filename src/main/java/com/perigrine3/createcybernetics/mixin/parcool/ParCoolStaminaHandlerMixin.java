package com.perigrine3.createcybernetics.mixin.parcool;

import com.alrex.parcool.common.attachment.common.ReadonlyStamina;
import com.perigrine3.createcybernetics.compat.parcool.ParcoolClientCompat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
        targets = "com.alrex.parcool.common.stamina.handlers.ParCoolStaminaHandler",
        remap = false
)
public abstract class ParCoolStaminaHandlerMixin {

    @ModifyVariable(
            method = "consume",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private int createcybernetics$reduceSynthLungsStaminaDrain(
            int value,
            LocalPlayer player,
            ReadonlyStamina current
    ) {
        return ParcoolClientCompat.modifySynthLungsStaminaDrain(player, value);
    }
}