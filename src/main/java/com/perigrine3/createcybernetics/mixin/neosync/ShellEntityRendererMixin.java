package com.perigrine3.createcybernetics.mixin.neosync;

import com.breakinblocks.neosync.client.render.entity.ShellEntityRenderer;
import com.perigrine3.createcybernetics.client.skin.SkinHighlightLayer;
import com.perigrine3.createcybernetics.client.skin.SkinLayerHandler;
import com.perigrine3.createcybernetics.mixin.client.LivingEntityRendererAccessor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShellEntityRenderer.class, remap = false)
public abstract class ShellEntityRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createcybernetics$addCyberwareLayers(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ShellEntityRenderer renderer = (ShellEntityRenderer)(Object)this;

        LivingEntityRendererAccessor<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> accessor =
                (LivingEntityRendererAccessor<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)(Object)renderer;

        accessor.createcybernetics$addLayer(new SkinLayerHandler(renderer));
        accessor.createcybernetics$addLayer(new SkinHighlightLayer(renderer));
    }
}