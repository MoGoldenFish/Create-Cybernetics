package com.perigrine3.createcybernetics.block.ironsspells;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HexcoreExperiencePickupHandler {
    private HexcoreExperiencePickupHandler() {}

    @SubscribeEvent
    public static void onPlayerPickupExperience(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ExperienceOrb experienceOrb = event.getOrb();

        HexcoreBlockEntity hexcoreBlockEntity =
                HexcoreBlockEntity.findNearestEligibleHexcore(serverLevel, experienceOrb);

        if (hexcoreBlockEntity == null) {
            return;
        }

        if (hexcoreBlockEntity.tryAbsorbExperienceOrb(experienceOrb)) {
            event.setCanceled(true);
        }
    }
}