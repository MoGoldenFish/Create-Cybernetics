package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class CyberwareAnvilRepairJeiCategory implements IRecipeCategory<CyberwareAnvilRepairJeiRecipe> {

    public static final RecipeType<CyberwareAnvilRepairJeiRecipe> RECIPE_TYPE = RecipeType.create(CreateCybernetics.MODID, "cyberware_anvil_repair", CyberwareAnvilRepairJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CyberwareAnvilRepairJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 72);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL));
    }

    @Override
    public RecipeType<CyberwareAnvilRepairJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createcybernetics.cyberware_anvil_repair");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CyberwareAnvilRepairJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 13, 19)
                .addItemStack(recipe.brokenCyberware())
                .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    tooltip.add(Component.translatable("jei.createcybernetics.cyberware_anvil_repair.broken").withStyle(ChatFormatting.DARK_RED));
                    tooltip.add(Component.translatable("jei.createcybernetics.cyberware_anvil_repair.durability", 0, recipe.maximumDurability()).withStyle(ChatFormatting.DARK_GRAY));
                });

        builder.addSlot(RecipeIngredientRole.INPUT, 58, 19)
                .addItemStack(recipe.repairMaterial())
                .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.createcybernetics.cyberware_anvil_repair.restores", recipe.repairAmount()).withStyle(ChatFormatting.DARK_GREEN)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 19)
                .addItemStack(recipe.repairedCyberware())
                .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    int displayedDurability = Math.min(recipe.maximumDurability(), recipe.repairAmount());
                    tooltip.add(Component.translatable("jei.createcybernetics.cyberware_anvil_repair.durability", displayedDurability, recipe.maximumDurability()).withStyle(ChatFormatting.DARK_GREEN));
                });
    }

    @Override
    public void draw(CyberwareAnvilRepairJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();

        guiGraphics.drawString(minecraft.font, "+", 41, 24, 0xFF808080, false);
        guiGraphics.drawString(minecraft.font, "→", 96, 24, 0xFF808080, false);

        Component repairText = Component.translatable("jei.createcybernetics.cyberware_anvil_repair.repair_amount", recipe.repairAmount());
        guiGraphics.drawString(minecraft.font, repairText, 5, 48, 0xFF00AA00, false);

        Component typeText = Component.translatable("jei.createcybernetics.cyberware_anvil_repair.type." + recipe.repairType().name().toLowerCase());
        guiGraphics.drawString(minecraft.font, typeText, 5, 60, 0xFF4a4a4a, false);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(CyberwareAnvilRepairJeiRecipe recipe) {
        ResourceLocation cyberwareId = BuiltInRegistries.ITEM.getKey(recipe.brokenCyberware().getItem());
        ResourceLocation materialId = BuiltInRegistries.ITEM.getKey(recipe.repairMaterial().getItem());

        return ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberware_anvil_repair/" + cyberwareId.getNamespace() + "/" + cyberwareId.getPath() + "/" + materialId.getNamespace() + "/" + materialId.getPath());
    }
}