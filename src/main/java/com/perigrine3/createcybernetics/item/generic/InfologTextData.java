package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class InfologTextData {
    private InfologTextData() {}

    public static final String KEY_TEXT = "cc_infolog_text";
    public static final String KEY_TITLE = "cc_infolog_title";
    public static final String KEY_LOCKED = "cc_infolog_locked";

    public static String getText(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            if (tag.contains(KEY_TEXT, CompoundTag.TAG_STRING)) {
                return tag.getString(KEY_TEXT);
            }
        }

        if (stack.getItem() instanceof InfologDataShardItem infolog) {
            return infolog.getDefaultText();
        }

        return "";
    }

    public static String getTitle(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            if (tag.contains(KEY_TITLE, CompoundTag.TAG_STRING)) {
                return tag.getString(KEY_TITLE);
            }
        }

        if (stack.getItem() instanceof InfologDataShardItem infolog) {
            return infolog.getDefaultTitle();
        }

        return "";
    }

    public static boolean isLocked(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            if (tag.contains(KEY_LOCKED, CompoundTag.TAG_BYTE)) {
                return tag.getBoolean(KEY_LOCKED);
            }
        }

        if (stack.getItem() instanceof InfologDataShardItem infolog) {
            return infolog.isPermanentlyLocked();
        }

        return false;
    }

    public static void setText(ItemStack stack, String text) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(KEY_TEXT, text == null ? "" : text);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setTitle(ItemStack stack, String title) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(KEY_TITLE, title == null ? "" : title);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setLocked(ItemStack stack, boolean locked) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putBoolean(KEY_LOCKED, locked);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }
}