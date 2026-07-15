package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.recipe.EngineeringTableRecipe;
import com.perigrine3.createcybernetics.recipe.GraftingTableRecipe;
import com.perigrine3.createcybernetics.recipe.ModRecipes;
import com.perigrine3.createcybernetics.screen.ModMenuTypes;
import com.perigrine3.createcybernetics.screen.custom.crafting.EngineeringTableMenu;
import com.perigrine3.createcybernetics.screen.custom.crafting.EngineeringTableScreen;
import com.perigrine3.createcybernetics.screen.custom.crafting.GraftingTableMenu;
import com.perigrine3.createcybernetics.screen.custom.crafting.GraftingTableScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.fml.ModList;

import java.util.List;

@JeiPlugin
public class JEICyberneticsPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        CreateCybernetics.LOGGER.info("[Create Cybernetics JEI] Registering recipe categories. Create loaded: {}", isCreateLoaded());

        registration.addRecipeCategories(
                new EngineeringTableRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new GraftingTableRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new DynamicPotionAutoinjectorCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );

        if (isCreateLoaded()) {
            registration.addRecipeCategories(
                    new DynamicPotionAutoinjectorFillingCategory(registration.getJeiHelpers().getGuiHelper())
            );
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        CreateCybernetics.LOGGER.info("[Create Cybernetics JEI] Registering recipes.");

        List<DynamicPotionAutoinjectorJeiRecipe> craftingRecipes =
                DynamicPotionAutoinjectorJeiRecipeFactory.createCraftingRecipes();

        CreateCybernetics.LOGGER.info(
                "[Create Cybernetics JEI] Dynamic potion autoinjector crafting recipes: {}",
                craftingRecipes.size()
        );

        registration.addRecipes(
                DynamicPotionAutoinjectorCraftingCategory.RECIPE_TYPE,
                craftingRecipes
        );

        if (isCreateLoaded()) {
            List<DynamicPotionAutoinjectorFillingJeiRecipe> fillingRecipes =
                    DynamicPotionAutoinjectorJeiRecipeFactory.createFillingRecipes();

            CreateCybernetics.LOGGER.info(
                    "[Create Cybernetics JEI] Dynamic potion autoinjector filling recipes: {}",
                    fillingRecipes.size()
            );

            registration.addRecipes(
                    DynamicPotionAutoinjectorFillingCategory.RECIPE_TYPE,
                    fillingRecipes
            );
        }

        if (Minecraft.getInstance().level == null) {
            CreateCybernetics.LOGGER.warn("[Create Cybernetics JEI] Client level is null; skipping table recipe-manager based recipes.");
            return;
        }

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<RecipeHolder<EngineeringTableRecipe>> engineeringTableRecipes =
                recipeManager.getAllRecipesFor(ModRecipes.ENGINEERING_TABLE_TYPE.get());

        CreateCybernetics.LOGGER.info(
                "[Create Cybernetics JEI] Engineering table recipes: {}",
                engineeringTableRecipes.size()
        );

        registration.addRecipes(
                EngineeringTableRecipeCategory.ENGINEERING_TABLE_RECIPE_TYPE,
                engineeringTableRecipes
        );

        List<RecipeHolder<GraftingTableRecipe>> graftingTableRecipes =
                recipeManager.getAllRecipesFor(ModRecipes.GRAFTING_TABLE_TYPE.get());

        CreateCybernetics.LOGGER.info(
                "[Create Cybernetics JEI] Grafting table recipes: {}",
                graftingTableRecipes.size()
        );

        registration.addRecipes(
                GraftingTableRecipeCategory.GRAFTING_TABLE_RECIPE_TYPE,
                graftingTableRecipes
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        CreateCybernetics.LOGGER.info("[Create Cybernetics JEI] Registering recipe catalysts.");

        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ENGINEERING_TABLE.get()),
                EngineeringTableRecipeCategory.ENGINEERING_TABLE_RECIPE_TYPE
        );

        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.GRAFTING_TABLE.get()),
                GraftingTableRecipeCategory.GRAFTING_TABLE_RECIPE_TYPE
        );

        registration.addRecipeCatalyst(
                new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                DynamicPotionAutoinjectorCraftingCategory.RECIPE_TYPE
        );

        registration.addRecipeCatalyst(
                new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get()),
                DynamicPotionAutoinjectorCraftingCategory.RECIPE_TYPE
        );

        if (isCreateLoaded()) {
            registration.addRecipeCatalyst(
                    new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get()),
                    DynamicPotionAutoinjectorFillingCategory.RECIPE_TYPE
            );

            registration.addRecipeCatalyst(
                    new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get()),
                    DynamicPotionAutoinjectorFillingCategory.RECIPE_TYPE
            );
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                EngineeringTableScreen.class,
                157, 103,
                16, 11,
                EngineeringTableRecipeCategory.ENGINEERING_TABLE_RECIPE_TYPE
        );

        registration.addRecipeClickArea(
                GraftingTableScreen.class,
                109, 38,
                14, 9,
                GraftingTableRecipeCategory.GRAFTING_TABLE_RECIPE_TYPE
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                EngineeringTableMenu.class,
                ModMenuTypes.ENGINEERING_TABLE_MENU.get(),
                EngineeringTableRecipeCategory.ENGINEERING_TABLE_RECIPE_TYPE,
                1, 25,
                26, 36
        );

        registration.addRecipeTransferHandler(
                GraftingTableMenu.class,
                ModMenuTypes.GRAFTING_TABLE_MENU.get(),
                GraftingTableRecipeCategory.GRAFTING_TABLE_RECIPE_TYPE,
                0, 7,
                8, 36
        );
    }

    private static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }
}