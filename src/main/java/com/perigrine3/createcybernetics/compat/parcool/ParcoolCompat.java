package com.perigrine3.createcybernetics.compat.parcool;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.ModList;

import java.util.Optional;

public final class ParcoolCompat {

    public static final String PARCOOL_MODID = "parcool";

    private static final ResourceLocation STAMINA_RECOVERY_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath(PARCOOL_MODID, "stamina_recovery");

    private static final ResourceLocation SYNTHLUNGS_STAMINA_RECOVERY_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "synthlungs_parcool_stamina_recovery"
            );

    private static final double SYNTHLUNGS_STAMINA_RECOVERY_BONUS = 0.75D;

    private ParcoolCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(PARCOOL_MODID);
    }

    public static void applySynthLungsStaminaRecovery(ServerPlayer player) {
        if (player == null) return;
        if (!isLoaded()) return;

        getStaminaRecoveryAttribute().ifPresent(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;

            if (instance.getModifier(SYNTHLUNGS_STAMINA_RECOVERY_MODIFIER) == null) {
                instance.addTransientModifier(new AttributeModifier(
                        SYNTHLUNGS_STAMINA_RECOVERY_MODIFIER,
                        SYNTHLUNGS_STAMINA_RECOVERY_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        });
    }

    public static void removeSynthLungsStaminaRecovery(ServerPlayer player) {
        if (player == null) return;
        if (!isLoaded()) return;

        getStaminaRecoveryAttribute().ifPresent(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;

            instance.removeModifier(SYNTHLUNGS_STAMINA_RECOVERY_MODIFIER);
        });
    }

    private static Optional<Holder.Reference<Attribute>> getStaminaRecoveryAttribute() {
        return BuiltInRegistries.ATTRIBUTE.getHolder(STAMINA_RECOVERY_ATTRIBUTE);
    }
}