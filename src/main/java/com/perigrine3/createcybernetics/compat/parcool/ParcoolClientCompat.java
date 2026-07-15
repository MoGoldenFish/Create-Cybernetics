package com.perigrine3.createcybernetics.compat.parcool;

import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.lungs.SynthLungsItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class ParcoolClientCompat {

    private static final double SYNTHLUNGS_STAMINA_DRAIN_MULTIPLIER = 0.50D;

    private ParcoolClientCompat() {
    }

    public static int modifySynthLungsStaminaDrain(LocalPlayer player, int originalCost) {
        if (player == null) return originalCost;
        if (originalCost <= 0) return originalCost;
        if (!ParcoolCompat.isLoaded()) return originalCost;

        PlayerCyberwareData data = PlayerCyberwareData.getForVisual(
                player,
                player.registryAccess()
        );

        if (!hasSynthLungsInstalled(data)) {
            return originalCost;
        }

        return Math.max(
                1,
                (int) Math.ceil(originalCost * SYNTHLUNGS_STAMINA_DRAIN_MULTIPLIER)
        );
    }

    private static boolean hasSynthLungsInstalled(PlayerCyberwareData data) {
        if (data == null || data.getAll() == null) {
            return false;
        }

        for (InstalledCyberware[] installedArray : data.getAll().values()) {
            if (installedArray == null) continue;

            for (InstalledCyberware installed : installedArray) {
                if (installed == null) continue;

                ItemStack stack = installed.getItem();
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof SynthLungsItem) {
                    return true;
                }
            }
        }

        return false;
    }
}