package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class SharedNavigationShardData {

    private SharedNavigationShardData() {}

    public static final String KEY_NETWORK_ID = "cc_shared_navigation_network_id";
    public static final String KEY_PENDING_MERGE_SOURCE = "cc_shared_navigation_pending_merge_source";

    public static UUID getNetworkId(ItemStack stack) {
        CompoundTag tag = getTag(stack);

        if (tag == null || !tag.hasUUID(KEY_NETWORK_ID)) {
            return null;
        }

        return tag.getUUID(KEY_NETWORK_ID);
    }

    public static UUID getOrCreateNetworkId(ItemStack stack) {
        UUID networkId = getNetworkId(stack);

        if (networkId != null) {
            return networkId;
        }

        networkId = UUID.randomUUID();
        setNetworkId(stack, networkId);
        return networkId;
    }

    public static void setNetworkId(ItemStack stack, UUID networkId) {
        if (stack == null || stack.isEmpty() || networkId == null) return;

        CompoundTag tag = getOrCreateTag(stack);
        tag.putUUID(KEY_NETWORK_ID, networkId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID getPendingMergeSource(ItemStack stack) {
        CompoundTag tag = getTag(stack);

        if (tag == null || !tag.hasUUID(KEY_PENDING_MERGE_SOURCE)) {
            return null;
        }

        return tag.getUUID(KEY_PENDING_MERGE_SOURCE);
    }

    public static void setPendingMergeSource(ItemStack stack, UUID sourceNetworkId) {
        if (stack == null || stack.isEmpty() || sourceNetworkId == null) return;

        CompoundTag tag = getOrCreateTag(stack);
        tag.putUUID(KEY_PENDING_MERGE_SOURCE, sourceNetworkId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearPendingMergeSource(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tag = getOrCreateTag(stack);
        tag.remove(KEY_PENDING_MERGE_SOURCE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isInitialized(ItemStack stack) {
        return getNetworkId(stack) != null;
    }

    private static CompoundTag getTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null ? tag : new CompoundTag();
    }
}