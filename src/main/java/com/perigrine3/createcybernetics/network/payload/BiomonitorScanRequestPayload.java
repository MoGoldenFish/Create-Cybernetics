package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.effect.HiddenEffectRules;
import com.perigrine3.createcybernetics.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public record BiomonitorScanRequestPayload(
        int targetEntityId
) implements CustomPacketPayload {

    private static final double BIOMONITOR_RANGE = 96.0D;
    private static final double BIOMONITOR_RANGE_SQUARED =
            BIOMONITOR_RANGE * BIOMONITOR_RANGE;

    public static final Type<BiomonitorScanRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "biomonitor_scan_request"
            ));

    public static final StreamCodec<ByteBuf, BiomonitorScanRequestPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BiomonitorScanRequestPayload decode(ByteBuf buffer) {
                    return new BiomonitorScanRequestPayload(buffer.readInt());
                }

                @Override
                public void encode(
                        ByteBuf buffer,
                        BiomonitorScanRequestPayload payload
                ) {
                    buffer.writeInt(payload.targetEntityId());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            BiomonitorScanRequestPayload payload,
            ServerPlayer player
    ) {
        PlayerCyberwareData cyberwareData =
                player.getData(ModAttachments.CYBERWARE);

        if (cyberwareData == null) {
            return;
        }

        if (!cyberwareData.hasSpecificItem(
                ModItems.EYEUPGRADES_BIOMONITOR.get(),
                CyberwareSlot.EYES
        )) {
            return;
        }

        Entity entity = player.level().getEntity(payload.targetEntityId());

        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        if (!target.isAlive()) {
            return;
        }

        if (target == player) {
            return;
        }

        if (player.distanceToSqr(target) > BIOMONITOR_RANGE_SQUARED) {
            return;
        }

        if (!player.hasLineOfSight(target)) {
            return;
        }

        boolean hasHungerData = target instanceof Player;
        int foodLevel = 0;
        float saturationLevel = 0.0F;

        if (target instanceof Player targetPlayer) {
            foodLevel = targetPlayer.getFoodData().getFoodLevel();
            saturationLevel = targetPlayer.getFoodData().getSaturationLevel();
        }

        List<BiomonitorVitalsPayload.EffectData> effects =
                new ArrayList<>();

        for (MobEffectInstance effect : target.getActiveEffects()) {
            /*
             * Effects hidden from the normal inventory UI are also hidden from
             * biomonitor scans. The effect list is maintained only once in
             * HiddenEffectRules.
             */
            if (HiddenEffectRules.isHidden(effect)) {
                continue;
            }

            effects.add(new BiomonitorVitalsPayload.EffectData(
                    effect.getEffect()
                            .value()
                            .getDisplayName()
                            .getString(),
                    effect.getAmplifier(),
                    effect.getDuration(),
                    effect.isInfiniteDuration()
            ));
        }

        PacketDistributor.sendToPlayer(
                player,
                new BiomonitorVitalsPayload(
                        target.getId(),
                        target.getHealth(),
                        target.getMaxHealth(),
                        target.getArmorValue(),
                        hasHungerData,
                        foodLevel,
                        saturationLevel,
                        effects
                )
        );
    }
}