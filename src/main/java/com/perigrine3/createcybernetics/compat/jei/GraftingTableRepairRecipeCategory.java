package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.screen.custom.crafting.GraftingTableMenu;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class GraftingTableRepairRecipeCategory implements IRecipeCategory<GraftingTableRepairJeiRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "grafting_table_repair");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/grafting_table_gui.png");

    public static final RecipeType<GraftingTableRepairJeiRecipe> GRAFTING_TABLE_REPAIR_RECIPE_TYPE =
            new RecipeType<>(UID, GraftingTableRepairJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public GraftingTableRepairRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 4, 3, 150, 78);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.GRAFTING_TABLE.get()));
    }

    @Override
    public RecipeType<GraftingTableRepairJeiRecipe> getRecipeType() {
        return GRAFTING_TABLE_REPAIR_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createcybernetics.grafting_table.repair_title");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GraftingTableRepairJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, GraftingTableMenu.Layout.IN00_X - 4, GraftingTableMenu.Layout.IN00_Y - 3)
                .addItemStack(recipe.damaged());

        builder.addSlot(RecipeIngredientRole.INPUT, GraftingTableMenu.Layout.TEAR_X - 4, GraftingTableMenu.Layout.TEAR_Y - 3)
                .addItemStack(new ItemStack(Items.GHAST_TEAR));

        builder.addSlot(RecipeIngredientRole.OUTPUT, GraftingTableMenu.Layout.OUT_X - 4, GraftingTableMenu.Layout.OUT_Y - 3)
                .addItemStack(recipe.repaired());
    }
}