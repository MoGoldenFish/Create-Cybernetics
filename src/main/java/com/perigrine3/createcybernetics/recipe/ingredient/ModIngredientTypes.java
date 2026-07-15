package com.perigrine3.createcybernetics.recipe.ingredient;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModIngredientTypes {
    private ModIngredientTypes() {
    }

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.INGREDIENT_TYPES,
                    CreateCybernetics.MODID
            );

    public static final Supplier<IngredientType<CompletedBiochipIngredient>> COMPLETED_BIOCHIP =
            INGREDIENT_TYPES.register(
                    "completed_biochip",
                    () -> new IngredientType<>(CompletedBiochipIngredient.CODEC)
            );
}