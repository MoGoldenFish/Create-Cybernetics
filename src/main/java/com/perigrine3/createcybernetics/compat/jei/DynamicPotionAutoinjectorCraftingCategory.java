package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class DynamicPotionAutoinjectorCraftingCategory implements IRecipeCategory<DynamicPotionAutoinjectorJeiRecipe> {
    private static final ResourceLocation CRAFTING_TABLE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    public static final int WIDTH = 116;
    public static final int HEIGHT = 54;

    public static final RecipeType<DynamicPotionAutoinjectorJeiRecipe> RECIPE_TYPE =
            RecipeType.create(
                    CreateCybernetics.MODID,
                    "dynamic_potion_autoinjector_crafting",
                    DynamicPotionAutoinjectorJeiRecipe.class
            );

    private final IDrawable background;
    private final IDrawable icon;

    public DynamicPotionAutoinjectorCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(
                CRAFTING_TABLE_TEXTURE,
                29,
                16,
                WIDTH,
                HEIGHT
        );

        this.icon = guiHelper.createDrawableItemStack(
                new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get())
        );
    }

    @Override
    public RecipeType<DynamicPotionAutoinjectorJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createcybernetics.dynamic_potion_autoinjector_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DynamicPotionAutoinjectorJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 19)
                .addItemStack(recipe.emptyAutoinjector());

        builder.addSlot(RecipeIngredientRole.INPUT, 19, 19)
                .addItemStack(recipe.potion());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
            DynamicPotionAutoinjectorJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
    }
}