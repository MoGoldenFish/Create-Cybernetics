package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.ModItems;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.foundation.gui.AllGuiTextures;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public class DynamicPotionAutoinjectorFillingCategory implements IRecipeCategory<DynamicPotionAutoinjectorFillingJeiRecipe> {
    public static final int WIDTH = 177;
    public static final int HEIGHT = 70;

    public static final RecipeType<DynamicPotionAutoinjectorFillingJeiRecipe> RECIPE_TYPE =
            RecipeType.create(
                    CreateCybernetics.MODID,
                    "dynamic_potion_autoinjector_filling",
                    DynamicPotionAutoinjectorFillingJeiRecipe.class
            );

    private final IDrawable background;
    private final IDrawable icon;
    private final AnimatedSpout spout;

    public DynamicPotionAutoinjectorFillingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.DYNAMIC_POTION_AUTOINJECTOR.get()));
        this.spout = new AnimatedSpout();
    }

    @Override
    public RecipeType<DynamicPotionAutoinjectorFillingJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createcybernetics.dynamic_potion_autoinjector_filling");
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
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            DynamicPotionAutoinjectorFillingJeiRecipe recipe,
            IFocusGroup focuses
    ) {
        builder
                .addSlot(RecipeIngredientRole.INPUT, 27, 51)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.emptyAutoinjector());

        CreateRecipeCategory.addFluidSlot(
                builder,
                27,
                32,
                createSizedPotionFluidIngredient(recipe)
        );

        builder
                .addSlot(RecipeIngredientRole.OUTPUT, 132, 51)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
            DynamicPotionAutoinjectorFillingJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);

        spout.withFluids(List.of(recipe.potionFluid()))
                .draw(graphics, getBackground().getWidth() / 2 - 13, 22);
    }

    private static SizedFluidIngredient createSizedPotionFluidIngredient(DynamicPotionAutoinjectorFillingJeiRecipe recipe) {
        return new SizedFluidIngredient(
                DataComponentFluidIngredient.of(false, recipe.potionFluid()),
                recipe.potionFluid().getAmount()
        );
    }
}