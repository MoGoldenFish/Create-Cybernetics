package com.perigrine3.createcybernetics.client.navigation;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.gui.NavigationMapScreen;
import com.perigrine3.createcybernetics.screen.custom.crafting.ExpandedInventoryScreen;
import com.perigrine3.createcybernetics.screen.custom.hud.CyberpunkMinimapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class NavigationInventoryButtonEvents {
    private static final int BUTTON_X_OFFSET = 77;
    private static final int BUTTON_Y_OFFSET = 44;

    private NavigationInventoryButtonEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Screen parentScreen = event.getScreen();

        if (player == null) return;
        if (!CyberpunkMinimapRenderer.hasNavigationChip(player)) return;

        int leftPos = (parentScreen.width - 176) / 2;
        int topPos = (parentScreen.height - 166) / 2;

        NavigationInventoryButton button = new NavigationInventoryButton(leftPos + BUTTON_X_OFFSET, topPos + BUTTON_Y_OFFSET, () -> minecraft.setScreen(new NavigationMapScreen(parentScreen)));

        event.addListener(button);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) return;

        for (var child : event.getScreen().children()) {
            if (!(child instanceof NavigationInventoryButton button)) continue;

            button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            button.renderTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }
}