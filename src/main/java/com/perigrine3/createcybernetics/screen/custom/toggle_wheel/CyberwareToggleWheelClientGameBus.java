package com.perigrine3.createcybernetics.screen.custom.toggle_wheel;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CyberwareToggleWheelClientGameBus {

    private CyberwareToggleWheelClientGameBus() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!CyberwareToggleWheelScreen.isWheelOpen()) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.screen != null) {
            CyberwareToggleWheelScreen.closeWheel();
            return;
        }

        KeyMapping attack = mc.options.keyAttack;
        if (attack != null && attack.isDown()) {
            attack.setDown(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!CyberwareToggleWheelScreen.isWheelOpen()) return;

        event.setCanceled(true);
        CyberwareToggleWheelScreen.scrollSelection(event.getScrollDeltaY());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!CyberwareToggleWheelScreen.isWheelOpen()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            event.setCanceled(true);

            KeyMapping attack = mc.options.keyAttack;
            if (attack != null) attack.setDown(false);

            CyberwareToggleWheelScreen.closeWheel();
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            event.setCanceled(true);

            KeyMapping attack = mc.options.keyAttack;
            if (attack != null) attack.setDown(false);

            CyberwareToggleWheelScreen.toggleSelected();
        }
    }
}