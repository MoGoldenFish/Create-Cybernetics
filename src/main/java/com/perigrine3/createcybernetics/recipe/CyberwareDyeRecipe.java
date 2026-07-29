package com.perigrine3.createcybernetics.recipe;

import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.item.generic.InfologDataShardItem;
import com.perigrine3.createcybernetics.util.SecondaryDyeColor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class CyberwareDyeRecipe extends CustomRecipe {

    public CyberwareDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        DyeRecipeContents contents = collectContents(input);

        return contents != null
                && !contents.target().isEmpty()
                && (!contents.primaryDyes().isEmpty() || !contents.secondaryDyes().isEmpty());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        DyeRecipeContents contents = collectContents(input);

        if (contents == null) {
            return ItemStack.EMPTY;
        }

        if (contents.target().isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (contents.primaryDyes().isEmpty() && contents.secondaryDyes().isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack output = contents.target().copy();
        output.setCount(1);

        if (!contents.primaryDyes().isEmpty()) {
            DyedItemColor existing =
                    output.get(DataComponents.DYED_COLOR);

            Integer existingColor =
                    existing == null
                            ? null
                            : existing.rgb();

            int primaryColor =
                    mixDyeRgb(
                            existingColor,
                            contents.primaryDyes()
                    );

            output.set(
                    DataComponents.DYED_COLOR,
                    new DyedItemColor(primaryColor, true)
            );
        }

        if (!contents.secondaryDyes().isEmpty()) {
            Integer existingColor =
                    SecondaryDyeColor.hasColor(output)
                            ? SecondaryDyeColor.getRgb(output)
                            : null;

            int secondaryColor =
                    mixDyeRgb(
                            existingColor,
                            contents.secondaryDyes()
                    );

            SecondaryDyeColor.setColor(
                    output,
                    secondaryColor
            );
        }

        return output;
    }

    private static DyeRecipeContents collectContents(CraftingInput input) {
        if (input.width() < 2 || input.width() > 3) {
            return null;
        }

        if (input.height() < 1 || input.height() > 3) {
            return null;
        }

        ItemStack target = ItemStack.EMPTY;
        int targetColumn = -1;
        int targetRow = -1;

        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                ItemStack stack =
                        input.getItem(index(input, column, row));

                if (stack.isEmpty()) {
                    continue;
                }

                if (stack.getItem() instanceof DyeItem) {
                    continue;
                }

                if (!target.isEmpty()) {
                    return null;
                }

                if (!isValidTarget(stack)) {
                    return null;
                }

                target = stack;
                targetColumn = column;
                targetRow = row;
            }
        }

        if (target.isEmpty() || targetColumn < 0 || targetRow < 0) {
            return null;
        }

        List<DyeItem> primaryDyes = new ArrayList<>();
        List<DyeItem> secondaryDyes = new ArrayList<>();

        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                ItemStack stack =
                        input.getItem(index(input, column, row));

                if (stack.isEmpty()) {
                    continue;
                }

                if (column == targetColumn && row == targetRow) {
                    continue;
                }

                if (!(stack.getItem() instanceof DyeItem dye)) {
                    return null;
                }

                if (column < targetColumn) {
                    primaryDyes.add(dye);
                    continue;
                }

                if (column > targetColumn) {
                    secondaryDyes.add(dye);
                    continue;
                }

                return null;
            }
        }

        if (primaryDyes.isEmpty() && secondaryDyes.isEmpty()) {
            return null;
        }

        return new DyeRecipeContents(
                target,
                primaryDyes,
                secondaryDyes
        );
    }

    private static int index(
            CraftingInput input,
            int column,
            int row
    ) {
        return column + row * input.width();
    }

    private static boolean isValidTarget(ItemStack stack) {
        if (stack.getItem() instanceof ICyberwareItem cyberwareItem) {
            return cyberwareItem.isDyeable(stack);
        }

        if (stack.getItem() instanceof InfologDataShardItem dataShardItem) {
            return dataShardItem.isDyeable(stack);
        }

        return false;
    }

    private static int mixDyeRgb(
            Integer existingRgb,
            List<DyeItem> dyes
    ) {
        int redTotal = 0;
        int greenTotal = 0;
        int blueTotal = 0;

        int brightnessTotal = 0;
        int count = 0;

        if (existingRgb != null) {
            int rgb = existingRgb & 0x00FFFFFF;

            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            redTotal += red;
            greenTotal += green;
            blueTotal += blue;

            brightnessTotal +=
                    Math.max(red, Math.max(green, blue));

            count++;
        }

        for (DyeItem dye : dyes) {
            int rgb =
                    dye.getDyeColor().getTextureDiffuseColor()
                            & 0x00FFFFFF;

            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            redTotal += red;
            greenTotal += green;
            blueTotal += blue;

            brightnessTotal +=
                    Math.max(red, Math.max(green, blue));

            count++;
        }

        if (count <= 0) {
            return 0xFFFFFF;
        }

        int redAverage = redTotal / count;
        int greenAverage = greenTotal / count;
        int blueAverage = blueTotal / count;

        float brightnessAverage =
                (float) brightnessTotal / (float) count;

        float maximumAverage =
                Math.max(
                        redAverage,
                        Math.max(greenAverage, blueAverage)
                );

        if (maximumAverage > 0.0F) {
            float scale =
                    brightnessAverage / maximumAverage;

            redAverage = (int) (redAverage * scale);
            greenAverage = (int) (greenAverage * scale);
            blueAverage = (int) (blueAverage * scale);
        }

        redAverage =
                Math.max(0, Math.min(255, redAverage));

        greenAverage =
                Math.max(0, Math.min(255, greenAverage));

        blueAverage =
                Math.max(0, Math.min(255, blueAverage));

        return (redAverage << 16)
                | (greenAverage << 8)
                | blueAverage;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CYBERWARE_DYE.get();
    }

    private record DyeRecipeContents(
            ItemStack target,
            List<DyeItem> primaryDyes,
            List<DyeItem> secondaryDyes
    ) {}
}