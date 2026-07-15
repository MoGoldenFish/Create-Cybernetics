package com.perigrine3.createcybernetics.effect.quickhacks;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.humanity.HumanityAttributeModifiers;
import com.perigrine3.createcybernetics.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberpsychosisQuickhackController {
    private CyberpsychosisQuickhackController() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        if (player.hasEffect(ModEffects.CYBERPSYCHOSIS_HACK)) {
            int maxHumanity = Math.max(1, HumanityAttributeModifiers.getBase(player));
            int penalty = Mth.ceil(maxHumanity * 0.75f);

            data.setHumanityPenalty(
                    player,
                    CyberpsychosisQuickhackEffect.HUMANITY_PENALTY_KEY,
                    penalty
            );
        } else {
            data.clearHumanityPenalty(
                    player,
                    CyberpsychosisQuickhackEffect.HUMANITY_PENALTY_KEY
            );
        }

        data.setDirty();
        ModAttachments.syncCyberware(player);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().is(ModEffects.CYBERPSYCHOSIS_HACK)) return;

        clearPenalty(player);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().is(ModEffects.CYBERPSYCHOSIS_HACK)) return;

        clearPenalty(player);
    }

    private static void clearPenalty(ServerPlayer player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        data.clearHumanityPenalty(
                player,
                CyberpsychosisQuickhackEffect.HUMANITY_PENALTY_KEY
        );

        data.setDirty();
        ModAttachments.syncCyberware(player);
    }
}