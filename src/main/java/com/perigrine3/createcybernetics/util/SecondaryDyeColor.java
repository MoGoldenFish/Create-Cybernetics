package com.perigrine3.createcybernetics.util;

import com.perigrine3.createcybernetics.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

public final class SecondaryDyeColor {
    private SecondaryDyeColor() {}

    public static boolean hasColor(ItemStack stack) {
        return stack.has(ModDataComponents.SECONDARY_DYED_COLOR.get());
    }

    public static int getColor(ItemStack stack) {
        Integer color = stack.get(ModDataComponents.SECONDARY_DYED_COLOR.get());

        if (color == null) {
            return 0xFFFFFFFF;
        }

        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    public static int getRgb(ItemStack stack) {
        Integer color = stack.get(ModDataComponents.SECONDARY_DYED_COLOR.get());

        if (color == null) {
            return 0xFFFFFF;
        }

        return color & 0x00FFFFFF;
    }

    public static void setColor(ItemStack stack, int color) {
        stack.set(
                ModDataComponents.SECONDARY_DYED_COLOR.get(),
                color & 0x00FFFFFF
        );
    }

    public static void removeColor(ItemStack stack) {
        stack.remove(ModDataComponents.SECONDARY_DYED_COLOR.get());
    }
}