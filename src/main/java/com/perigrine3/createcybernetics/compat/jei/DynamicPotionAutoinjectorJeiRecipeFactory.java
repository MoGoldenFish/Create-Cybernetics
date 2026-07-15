package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.component.ModDataComponents;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.DynamicPotionAutoinjectorRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DynamicPotionAutoinjectorJeiRecipeFactory {
    private static final ResourceLocation CREATE_POTION_FLUID_ID =
            ResourceLocation.fromNamespaceAndPath("create", "potion");

    private DynamicPotionAutoinjectorJeiRecipeFactory() {}

    public static List<DynamicPotionAutoinjectorJeiRecipe> createCraftingRecipes() {
        List<DynamicPotionAutoinjectorJeiRecipe> recipes = new ArrayList<>();

        BuiltInRegistries.POTION.holders().forEach(potionHolder -> {
            PotionContents contents = new PotionContents(potionHolder);

            if (!DynamicPotionAutoinjectorRules.isValidDynamicPotionContents(contents)) {
                return;
            }

            ItemStack potionStack = PotionContents.createItemStack(Items.POTION, potionHolder);
            ItemStack result = createPotionAutoinjector(contents);

            if (result.isEmpty()) {
                return;
            }

            recipes.add(new DynamicPotionAutoinjectorJeiRecipe(
                    new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                    potionStack,
                    result
            ));
        });

        if (recipes.isEmpty()) {
            addFallbackCraftingRecipe(recipes);
        }

        return recipes;
    }

    public static List<DynamicPotionAutoinjectorFillingJeiRecipe> createFillingRecipes() {
        Optional<Fluid> createPotionFluid = getCreatePotionFluid();

        if (createPotionFluid.isEmpty()) {
            return List.of();
        }

        List<DynamicPotionAutoinjectorFillingJeiRecipe> recipes = new ArrayList<>();
        Fluid potionFluid = createPotionFluid.get();

        BuiltInRegistries.POTION.holders().forEach(potionHolder -> {
            PotionContents contents = new PotionContents(potionHolder);

            if (!DynamicPotionAutoinjectorRules.isValidDynamicPotionContents(contents)) {
                return;
            }

            FluidStack fluidStack = new FluidStack(
                    potionFluid,
                    DynamicPotionAutoinjectorRules.REQUIRED_POTION_AMOUNT
            );
            fluidStack.set(DataComponents.POTION_CONTENTS, contents);

            ItemStack result = createPotionAutoinjector(contents);

            if (result.isEmpty()) {
                return;
            }

            recipes.add(new DynamicPotionAutoinjectorFillingJeiRecipe(
                    new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                    fluidStack,
                    result
            ));
        });

        if (recipes.isEmpty()) {
            addFallbackFillingRecipe(recipes, potionFluid);
        }

        return recipes;
    }

    private static void addFallbackCraftingRecipe(List<DynamicPotionAutoinjectorJeiRecipe> recipes) {
        PotionContents contents = new PotionContents(Potions.SWIFTNESS);

        ItemStack potionStack = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        ItemStack result = createPotionAutoinjector(contents);

        if (result.isEmpty()) {
            return;
        }

        recipes.add(new DynamicPotionAutoinjectorJeiRecipe(
                new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                potionStack,
                result
        ));
    }

    private static void addFallbackFillingRecipe(List<DynamicPotionAutoinjectorFillingJeiRecipe> recipes, Fluid potionFluid) {
        PotionContents contents = new PotionContents(Potions.SWIFTNESS);

        FluidStack fluidStack = new FluidStack(
                potionFluid,
                DynamicPotionAutoinjectorRules.REQUIRED_POTION_AMOUNT
        );
        fluidStack.set(DataComponents.POTION_CONTENTS, contents);

        ItemStack result = createPotionAutoinjector(contents);

        if (result.isEmpty()) {
            return;
        }

        recipes.add(new DynamicPotionAutoinjectorFillingJeiRecipe(
                new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                fluidStack,
                result
        ));
    }

    private static Optional<Fluid> getCreatePotionFluid() {
        return BuiltInRegistries.FLUID.getOptional(CREATE_POTION_FLUID_ID);
    }

    private static ItemStack createPotionAutoinjector(PotionContents contents) {
        ItemStack result = new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get());
        result.set(ModDataComponents.POTION_AUTOINJECTOR_CONTENTS.get(), contents);
        return result;
    }
}