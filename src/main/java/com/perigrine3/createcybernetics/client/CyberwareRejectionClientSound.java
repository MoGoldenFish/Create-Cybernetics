package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.effect.ModEffects;
import com.perigrine3.createcybernetics.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberwareRejectionClientSound {

    private static int nextAttemptTick = -1;

    private CyberwareRejectionClientSound() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }

        MobEffectInstance rejection = getEffect(player, ModEffects.CYBERWARE_REJECTION);
        if (rejection == null) {
            nextAttemptTick = -1;
            return;
        }

        int now = player.tickCount;
        int level = Mth.clamp(rejection.getAmplifier() + 1, 1, 3);

        int minInterval;
        int maxInterval;
        int chanceDenom;
        float volume;

        if (level <= 1) {
            minInterval = 8 * 20;
            maxInterval = 16 * 20;
            chanceDenom = 5;
            volume = 0.45F;
        } else if (level == 2) {
            minInterval = 4 * 20;
            maxInterval = 9 * 20;
            chanceDenom = 3;
            volume = 0.75F;
        } else {
            minInterval = 2 * 20;
            maxInterval = 5 * 20;
            chanceDenom = 2;
            volume = 1.0F;
        }

        if (nextAttemptTick < 0) {
            nextAttemptTick = now + Mth.nextInt(player.getRandom(), minInterval, maxInterval);
            return;
        }

        if (now < nextAttemptTick) {
            return;
        }

        nextAttemptTick = now + Mth.nextInt(player.getRandom(), minInterval, maxInterval);

        if (player.getRandom().nextInt(chanceDenom) != 0) {
            return;
        }

        player.level().playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.GLITCHY.get(),
                SoundSource.PLAYERS,
                volume,
                0.85F + player.getRandom().nextFloat() * 0.30F,
                false
        );
    }

    private static MobEffectInstance getEffect(LocalPlayer player, Holder<MobEffect> effect) {
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst != null && inst.is(effect)) {
                return inst;
            }
        }

        return null;
    }
}