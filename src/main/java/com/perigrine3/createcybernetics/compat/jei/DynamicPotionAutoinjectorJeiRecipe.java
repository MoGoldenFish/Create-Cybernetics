package com.perigrine3.createcybernetics.compat.jei;

import net.minecraft.world.item.ItemStack;

public record DynamicPotionAutoinjectorJeiRecipe(
        ItemStack emptyAutoinjector,
        ItemStack potion,
        ItemStack result
) {}