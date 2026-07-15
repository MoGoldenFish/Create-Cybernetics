package com.perigrine3.createcybernetics.effect;

import com.perigrine3.createcybernetics.common.humanity.DataIntegrityHandler;
import com.perigrine3.createcybernetics.common.humanity.HumanityAttributeModifiers;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public enum CyberpsychosisSeverity {
    NONE(0),
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3);

    public static final float LEVEL_1_START_PERCENT = 0.24F;
    public static final float LEVEL_2_START_PERCENT = 0.15F;
    public static final float LEVEL_3_START_PERCENT = 0.0F;
    public static final float LEVEL_3_NEGATIVE_CAP_PERCENT = -0.50F;

    private final int id;

    CyberpsychosisSeverity(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public boolean isAtLeast(CyberpsychosisSeverity other) {
        return this.id >= other.id;
    }

    public static CyberpsychosisSeverity fromPlayer(Player player) {
        if (player == null) {
            return NONE;
        }

        if (DataIntegrityHandler.usesDataIntegrity(player)) {
            CyberpsychosisSeverity integritySeverity = fromDataIntegrityPercent(getDataIntegrityPercent(player));

            if (DataIntegrityHandler.hasMissedBootDown(player) && integritySeverity.id() < LEVEL_1.id()) {
                return LEVEL_1;
            }

            return integritySeverity;
        }

        return fromPercent(getHumanityPercent(player));
    }

    public static CyberpsychosisSeverity fromPercent(float percent) {
        if (percent < LEVEL_3_START_PERCENT) {
            return LEVEL_3;
        }

        if (percent <= LEVEL_2_START_PERCENT) {
            return LEVEL_2;
        }

        if (percent <= LEVEL_1_START_PERCENT) {
            return LEVEL_1;
        }

        return NONE;
    }

    public static CyberpsychosisSeverity fromDataIntegrityPercent(float percent) {
        if (percent <= LEVEL_3_START_PERCENT) {
            return LEVEL_3;
        }

        if (percent <= LEVEL_2_START_PERCENT) {
            return LEVEL_2;
        }

        if (percent <= LEVEL_1_START_PERCENT) {
            return LEVEL_1;
        }

        return NONE;
    }

    public static int getCurrentHumanity(Player player) {
        if (player == null) {
            return 0;
        }

        if (DataIntegrityHandler.usesDataIntegrity(player)) {
            return DataIntegrityHandler.getIntegrity(player);
        }

        return HumanityAttributeModifiers.get(player);
    }

    public static int getMaxHumanity(Player player) {
        if (player == null) {
            return Math.max(1, HumanityAttributeModifiers.getConfiguredBaseHumanity());
        }

        if (DataIntegrityHandler.usesDataIntegrity(player)) {
            return Math.max(1, DataIntegrityHandler.getMaxIntegrity());
        }

        return Math.max(1, HumanityAttributeModifiers.getBase(player));
    }

    public static float getHumanityPercent(Player player) {
        int current = getCurrentHumanity(player);
        int max = getMaxHumanity(player);

        return current / (float) max;
    }

    public static float getDataIntegrityPercent(Player player) {
        return DataIntegrityHandler.getIntegrityPercent(player);
    }

    public static float getPositiveDangerProgress(Player player) {
        float percent = DataIntegrityHandler.usesDataIntegrity(player)
                ? getDataIntegrityPercent(player)
                : getHumanityPercent(player);

        float progress = (LEVEL_1_START_PERCENT - percent) / LEVEL_1_START_PERCENT;
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static float getNegativeProgress(Player player) {
        if (DataIntegrityHandler.usesDataIntegrity(player)) {
            return 0.0F;
        }

        float percent = getHumanityPercent(player);

        if (percent >= LEVEL_3_START_PERCENT) {
            return 0.0F;
        }

        float progress = (LEVEL_3_START_PERCENT - percent)
                / (LEVEL_3_START_PERCENT - LEVEL_3_NEGATIVE_CAP_PERCENT);

        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static boolean isInRejectionRange(Player player) {
        return fromPlayer(player) != NONE;
    }

    public static boolean isInFugueRange(Player player) {
        return fromPlayer(player) == LEVEL_3;
    }
}
