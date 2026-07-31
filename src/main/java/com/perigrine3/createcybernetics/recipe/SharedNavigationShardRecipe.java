package com.perigrine3.createcybernetics.recipe;

import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.SharedNavigationShardData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class SharedNavigationShardRecipe extends CustomRecipe {

    public SharedNavigationShardRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        RecipeContents contents = collectContents(input);

        if (contents == null) return false;

        return SharedNavigationShardData.isInitialized(contents.first()) || SharedNavigationShardData.isInitialized(contents.second());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        RecipeContents contents = collectContents(input);

        if (contents == null) {
            return ItemStack.EMPTY;
        }

        UUID firstNetwork = SharedNavigationShardData.getNetworkId(contents.first());
        UUID secondNetwork = SharedNavigationShardData.getNetworkId(contents.second());

        if (firstNetwork == null && secondNetwork == null) {
            return ItemStack.EMPTY;
        }

        UUID targetNetwork;
        UUID mergeSource = null;

        if (firstNetwork == null) {
            targetNetwork = secondNetwork;
        } else if (secondNetwork == null) {
            targetNetwork = firstNetwork;
        } else if (firstNetwork.equals(secondNetwork)) {
            targetNetwork = firstNetwork;
        } else {
            targetNetwork = canonicalNetwork(firstNetwork, secondNetwork);
            mergeSource = targetNetwork.equals(firstNetwork) ? secondNetwork : firstNetwork;
        }

        ItemStack output = new ItemStack(ModItems.DATA_SHARD_SHARED_NAVIGATION.get(), 2);
        SharedNavigationShardData.setNetworkId(output, targetNetwork);

        if (mergeSource != null) {
            SharedNavigationShardData.setPendingMergeSource(output, mergeSource);
        }

        return output;
    }

    private static RecipeContents collectContents(CraftingInput input) {
        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);

            if (stack.isEmpty()) continue;
            if (!stack.is(ModItems.DATA_SHARD_SHARED_NAVIGATION.get())) return null;

            if (first.isEmpty()) {
                first = stack;
                continue;
            }

            if (second.isEmpty()) {
                second = stack;
                continue;
            }

            return null;
        }

        if (first.isEmpty() || second.isEmpty()) {
            return null;
        }

        return new RecipeContents(first, second);
    }

    private static UUID canonicalNetwork(UUID first, UUID second) {
        return first.toString().compareTo(second.toString()) <= 0 ? first : second;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHARED_NAVIGATION_SHARD.get();
    }

    private record RecipeContents(ItemStack first, ItemStack second) {}
}