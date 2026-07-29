package com.perigrine3.createcybernetics.recipe;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    private ModRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateCybernetics.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CyberwareDyeRecipe>> CYBERWARE_DYE =
            SERIALIZERS.register("cyberware_dye",
                    () -> new SimpleCraftingRecipeSerializer<>(CyberwareDyeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SharedNavigationShardRecipe>> SHARED_NAVIGATION_SHARD =
            SERIALIZERS.register("shared_navigation_shard",
                    () -> new SimpleCraftingRecipeSerializer<>(SharedNavigationShardRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicPotionAutoinjectorRecipe>> DYNAMIC_POTION_AUTOINJECTOR =
            SERIALIZERS.register("dynamic_potion_autoinjector",
                    () -> new SimpleCraftingRecipeSerializer<>(DynamicPotionAutoinjectorRecipe::new));


    public static final DeferredHolder<RecipeSerializer<?>, CyberbrainSmithingRecipe.Serializer> CYBERBRAIN_SMITHING =
            SERIALIZERS.register("cyberbrain_smithing", CyberbrainSmithingRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}