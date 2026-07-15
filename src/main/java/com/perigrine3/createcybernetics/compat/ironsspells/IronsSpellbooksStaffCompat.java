package com.perigrine3.createcybernetics.compat.ironsspells;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class IronsSpellbooksStaffCompat {
    private IronsSpellbooksStaffCompat() {}

    private static final String STAFF_ITEM_CLASS =
            "io.redspace.ironsspellbooks.item.weapons.StaffItem";

    private static boolean staffClassLookedUp;
    private static Class<?> staffItemClass;

    public static boolean isIronsSpellbooksStaff(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (!IronsSpellbooksCompat.isLoaded()) {
            return false;
        }

        if (resolveStaffItemClass()
                && staffItemClass.isInstance(stack.getItem())) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        return IronsSpellbooksCompat.MODID.equals(itemId.getNamespace())
                && itemId.getPath().contains("staff");
    }

    private static boolean resolveStaffItemClass() {
        if (staffClassLookedUp) {
            return staffItemClass != null;
        }

        staffClassLookedUp = true;

        try {
            staffItemClass = Class.forName(STAFF_ITEM_CLASS);
            return true;
        } catch (Throwable ignored) {
            staffItemClass = null;
            return false;
        }
    }
}