package com.perigrine3.createcybernetics.mixin.neosync;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.perigrine3.createcybernetics.compat.neosync.NeoSyncCyberwareComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShellState.class, remap = false)
public abstract class ShellStateMixin {

    @Inject(method = "empty(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/DyeColor;)Lcom/breakinblocks/neosync/api/shell/ShellState;", at = @At("RETURN"))
    private static void createcybernetics$initializeDefaultCyberware(ServerPlayer player, BlockPos pos, DyeColor color, CallbackInfoReturnable<ShellState> cir) {
        ShellState state = cir.getReturnValue();
        if (state == null) {
            return;
        }

        NeoSyncCyberwareComponent component = state.getComponent().as(NeoSyncCyberwareComponent.class);
        if (component != null) {
            component.initializeDefaultOrgans(player.registryAccess());
        }
    }
}