package com.perigrine3.createcybernetics.mixin;

import com.perigrine3.createcybernetics.common.command.CommandTeleportContext;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Inject(
            method = "executeCommandInContext",
            at = @At("HEAD")
    )
    private static void createcybernetics$beginCommandTeleportContext(
            CallbackInfo ci
    ) {
        CommandTeleportContext.beginCommand();
    }

    @Inject(
            method = "executeCommandInContext",
            at = @At("RETURN")
    )
    private static void createcybernetics$endCommandTeleportContext(
            CallbackInfo ci
    ) {
        CommandTeleportContext.endCommand();
    }
}