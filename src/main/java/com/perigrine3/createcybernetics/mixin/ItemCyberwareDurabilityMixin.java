package com.perigrine3.createcybernetics.mixin;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityData;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemCyberwareDurabilityMixin {

    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$isBarVisible(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CyberwareSlot slot = findDurabilitySlot(stack);
        if (slot == null) return;

        int maxDurability = CyberwareDurabilityData.getMaxDurability(stack, slot);
        int durability = CyberwareDurabilityData.getDurability(stack, slot);

        cir.setReturnValue(maxDurability > 0 && durability < maxDurability);
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$getBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        CyberwareSlot slot = findDurabilitySlot(stack);
        if (slot == null) return;

        int maxDurability = CyberwareDurabilityData.getMaxDurability(stack, slot);
        int durability = CyberwareDurabilityData.getDurability(stack, slot);

        if (maxDurability <= 0) {
            cir.setReturnValue(13);
            return;
        }

        cir.setReturnValue(Math.round(13.0F * durability / maxDurability));
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$getBarColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        CyberwareSlot slot = findDurabilitySlot(stack);
        if (slot == null) return;

        int maxDurability = CyberwareDurabilityData.getMaxDurability(stack, slot);
        int durability = CyberwareDurabilityData.getDurability(stack, slot);

        if (maxDurability <= 0) {
            cir.setReturnValue(0x00FF00);
            return;
        }

        float percent = Mth.clamp((float) durability / (float) maxDurability, 0.0F, 1.0F);
        cir.setReturnValue(Mth.hsvToRgb(percent / 3.0F, 1.0F, 1.0F));
    }

    private static CyberwareSlot findDurabilitySlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) return null;

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            if (!cyberwareItem.supportsSlot(slot)) continue;
            if (cyberwareItem.getMaxCyberwareDurability(stack, slot) <= 0) continue;

            return slot;
        }

        return null;
    }
}