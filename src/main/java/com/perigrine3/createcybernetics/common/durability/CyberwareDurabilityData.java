package com.perigrine3.createcybernetics.common.durability;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CyberwareDurabilityData {

    private static final String NBT_DURABILITY = "CreateCyberneticsDurability";
    private static final String NBT_REPAIR_FATIGUE = "CreateCyberneticsRepairFatigue";

    private CyberwareDurabilityData() {}

    public static boolean supportsDurability(ItemStack stack, CyberwareSlot slot) {
        if (stack == null || stack.isEmpty()) return false;
        if (slot == null) return false;
        if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) return false;

        return cyberwareItem.getMaxCyberwareDurability(stack, slot) > 0;
    }

    public static boolean isBiological(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        return stack.is(ModTags.Items.WETWARE_ITEM);
    }

    public static boolean isBiological(ItemStack stack, CyberwareSlot slot) {
        return isBiological(stack) && supportsDurability(stack, slot);
    }

    public static int getMaxDurability(ItemStack stack, CyberwareSlot slot) {
        if (stack == null || stack.isEmpty()) return 0;
        if (slot == null) return 0;
        if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) return 0;

        return Math.max(1, cyberwareItem.getMaxCyberwareDurability(stack, slot));
    }

    public static int getDurability(ItemStack stack, CyberwareSlot slot) {
        int maxDurability = getMaxDurability(stack, slot);
        if (maxDurability <= 0) return 0;

        CompoundTag tag = getTag(stack);

        if (!tag.contains(NBT_DURABILITY)) {
            return maxDurability;
        }

        return Mth.clamp(tag.getInt(NBT_DURABILITY), 0, maxDurability);
    }

    public static void setDurability(ItemStack stack, CyberwareSlot slot, int durability) {
        if (stack == null || stack.isEmpty()) return;

        int maxDurability = getMaxDurability(stack, slot);
        if (maxDurability <= 0) return;

        CompoundTag tag = getTag(stack);
        tag.putInt(NBT_DURABILITY, Mth.clamp(durability, 0, maxDurability));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getRepairFatigue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        return Mth.clamp(getTag(stack).getInt(NBT_REPAIR_FATIGUE), 0, 100);
    }

    public static void setRepairFatigue(ItemStack stack, int fatigue) {
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tag = getTag(stack);
        tag.putInt(NBT_REPAIR_FATIGUE, Mth.clamp(fatigue, 0, 100));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void fullyRepair(ItemStack stack, CyberwareSlot slot) {
        if (stack == null || stack.isEmpty()) return;
        if (!isBiological(stack, slot)) return;

        setDurability(stack, slot, getMaxDurability(stack, slot));
        setRepairFatigue(stack, 0);
    }

    public static boolean isDamaged(ItemStack stack, CyberwareSlot slot) {
        if (!supportsDurability(stack, slot)) return false;

        int maxDurability = getMaxDurability(stack, slot);
        return getDurability(stack, slot) < maxDurability;
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }
}