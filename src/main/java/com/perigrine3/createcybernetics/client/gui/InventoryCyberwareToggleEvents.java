package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class InventoryCyberwareToggleEvents {

    private InventoryCyberwareToggleEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        InventoryCyberwareToggleButtons.addTo(screen, event::addListener);
    }
}
