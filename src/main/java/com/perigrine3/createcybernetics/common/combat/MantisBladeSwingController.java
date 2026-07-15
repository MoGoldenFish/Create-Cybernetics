package com.perigrine3.createcybernetics.common.combat;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.arm.MantisBladeItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MantisBladeSwingController {

    private static final Map<UUID, Boolean> NEXT_OFFHAND = new HashMap<>();
    private static final Map<UUID, InteractionHand> PENDING_HAND = new HashMap<>();

    private MantisBladeSwingController() {
    }

    public static void requestNextSwing(Player player) {
        if (player == null) {
            return;
        }

        UUID id = player.getUUID();

        if (!hasEnabledMantisBladeInBothArms(player)) {
            NEXT_OFFHAND.remove(id);
            PENDING_HAND.remove(id);
            return;
        }

        boolean useOffhand = NEXT_OFFHAND.getOrDefault(id, false);
        NEXT_OFFHAND.put(id, !useOffhand);

        PENDING_HAND.put(id, useOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public static InteractionHand consumePendingSwing(Player player, InteractionHand originalHand) {
        if (player == null) {
            return originalHand;
        }

        UUID id = player.getUUID();
        InteractionHand pending = PENDING_HAND.remove(id);

        if (pending == null) {
            return originalHand;
        }

        return pending;
    }

    private static boolean hasEnabledMantisBladeInBothArms(Player player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            return false;
        }

        return hasEnabledMantisBladeInSlot(data, CyberwareSlot.LARM)
                && hasEnabledMantisBladeInSlot(data, CyberwareSlot.RARM);
    }

    private static boolean hasEnabledMantisBladeInSlot(PlayerCyberwareData data, CyberwareSlot slot) {
        if (data == null || slot == null) {
            return false;
        }

        InstalledCyberware[] arr = data.getAll().get(slot);

        if (arr == null) {
            return false;
        }

        for (int i = 0; i < arr.length; i++) {
            InstalledCyberware installed = arr[i];

            if (installed == null) {
                continue;
            }

            ItemStack stack = installed.getItem();

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof MantisBladeItem)) {
                continue;
            }

            int index = installed.getIndex();

            if (index < 0) {
                index = i;
            }

            if (!data.isEnabled(slot, index)) {
                continue;
            }

            return true;
        }

        return false;
    }
}