package com.perigrine3.createcybernetics.mixin;

import com.perigrine3.createcybernetics.compat.corpse.CorpseCompat;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.maxhenkel.corpse.entities.CorpseEntity")
public abstract class CorpseEntityRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void createcybernetics$dropCyberwareOnRemove(
            @Coerce Enum<?> reason,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        if (self.level().isClientSide()) {
            return;
        }

        if (!createcybernetics$shouldDropCyberware(reason)) {
            return;
        }

        for (ItemStack stack : CorpseCompat.getCorpseCyberwareItems(self)) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            Containers.dropItemStack(self.level(), self.getX(), self.getY(), self.getZ(), stack.copy());
        }

        CorpseCompat.writeCorpseCyberwareItems(
                self,
                NonNullList.withSize(CorpseCompat.CYBERWARE_SLOT_COUNT, ItemStack.EMPTY)
        );
    }

    private static boolean createcybernetics$shouldDropCyberware(Enum<?> reason) {
        if (reason == null) {
            return true;
        }

        String name = reason.name();

        return name.equals("KILLED")
                || name.equals("DISCARDED")
                || name.equals("DESTROYED");
    }
}