package com.perigrine3.createcybernetics.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record DynamicPotionAutoinjectorFillingJeiRecipe(
        ItemStack emptyAutoinjector,
        FluidStack potionFluid,
        ItemStack result
) {}