package com.perigrine3.createcybernetics.mixin.neosync;

import com.perigrine3.createcybernetics.compat.neosync.NeoSyncShellContainerAccess;
import com.perigrine3.createcybernetics.compat.neosync.NeoSyncShellPhasingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class NeoSyncShellContainerCollisionMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$allowPlayerThroughEmptyShellContainer(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(level instanceof Level world)) {
            return;
        }

        if (!(context instanceof EntityCollisionContext entityContext)) {
            return;
        }

        Entity entity = entityContext.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!NeoSyncShellContainerAccess.isEmpty(world, pos)) {
            return;
        }

        if (!player.isShiftKeyDown() && !NeoSyncShellPhasingHandler.isPhasing(player)) {
            return;
        }

        cir.setReturnValue(Shapes.empty());
    }
}