package com.perigrine3.createcybernetics.screen.custom.vampyres;

import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class VampyresPotionSlot extends Slot {

    public VampyresPotionSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return VampyresItem.isInjectable(stack);
    }

    @Override
    public int getMaxStackSize() {
        return VampyresItem.SLOT_STACK_LIMIT;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return VampyresItem.SLOT_STACK_LIMIT;
    }
}