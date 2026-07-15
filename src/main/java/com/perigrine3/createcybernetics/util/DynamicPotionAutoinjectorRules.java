package com.perigrine3.createcybernetics.util;

import com.perigrine3.createcybernetics.component.ModDataComponents;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Set;

public final class DynamicPotionAutoinjectorRules {
    public static final int REQUIRED_POTION_AMOUNT = 250;

    private static final Set<ResourceLocation> BLOCKED_POTIONS = Set.of(
            ResourceLocation.fromNamespaceAndPath("createcybernetics", "neuropozyne_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "roid_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "stim_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "blacklace_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "immunoboost_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "warp_potion"),
            ResourceLocation.fromNamespaceAndPath("cyberchems", "addictol_potion")
    );

    private DynamicPotionAutoinjectorRules() {
    }

    public static boolean isValidDynamicPotionContents(PotionContents contents) {
        if (contents == PotionContents.EMPTY) {
            return false;
        }

        if (!hasEffects(contents)) {
            return false;
        }

        return !isBlockedPotion(contents);
    }

    public static boolean isValidDynamicPotionFluid(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return false;
        }

        if (fluidStack.getAmount() < REQUIRED_POTION_AMOUNT) {
            return false;
        }

        PotionContents contents = fluidStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return isValidDynamicPotionContents(contents);
    }

    public static boolean isBlockedPotion(PotionContents contents) {
        if (contents == PotionContents.EMPTY) {
            return false;
        }

        return contents.potion()
                .flatMap(holder -> holder.unwrapKey())
                .map(key -> BLOCKED_POTIONS.contains(key.location()))
                .orElse(false);
    }

    public static ItemStack createAutoinjectorFromPotionContents(PotionContents contents) {
        if (!isValidDynamicPotionContents(contents)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get());
        result.set(ModDataComponents.POTION_AUTOINJECTOR_CONTENTS.get(), contents);
        return result;
    }

    public static ItemStack createAutoinjectorFromPotionFluid(FluidStack fluidStack) {
        if (!isValidDynamicPotionFluid(fluidStack)) {
            return ItemStack.EMPTY;
        }

        PotionContents contents = fluidStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return createAutoinjectorFromPotionContents(contents);
    }

    private static boolean hasEffects(PotionContents contents) {
        for (MobEffectInstance ignored : contents.getAllEffects()) {
            return true;
        }

        return false;
    }
}