package com.perigrine3.createcybernetics.recipe;

import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.DynamicPotionAutoinjectorRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class DynamicPotionAutoinjectorRecipe extends CustomRecipe {

    private static final int REQUIRED_EMPTY_AUTOINJECTORS = 1;
    private static final int RESULT_COUNT = 1;

    public DynamicPotionAutoinjectorRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int emptyAutoinjectors = 0;
        int validPotionItems = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ModItems.EMPTY_AUTOINJECTOR.get())) {
                emptyAutoinjectors++;
                continue;
            }

            if (isValidPotionIngredient(stack)) {
                validPotionItems++;
                continue;
            }

            return false;
        }

        return emptyAutoinjectors == REQUIRED_EMPTY_AUTOINJECTORS && validPotionItems == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack potionStack = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (isValidPotionIngredient(stack)) {
                potionStack = stack;
                break;
            }
        }

        if (potionStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PotionContents contents = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return DynamicPotionAutoinjectorRules.createAutoinjectorFromPotionContents(contents).copyWithCount(RESULT_COUNT);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DYNAMIC_POTION_AUTOINJECTOR.get();
    }

    private static boolean isValidPotionIngredient(ItemStack stack) {
        if (!stack.is(Tags.Items.POTIONS)) {
            return false;
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return DynamicPotionAutoinjectorRules.isValidDynamicPotionContents(contents);
    }
}