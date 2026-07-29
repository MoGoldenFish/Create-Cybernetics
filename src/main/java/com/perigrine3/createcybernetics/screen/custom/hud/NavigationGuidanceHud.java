package com.perigrine3.createcybernetics.screen.custom.hud;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NavigationGuidanceHud {

    private NavigationGuidanceHud() {}

    private static final ResourceLocation GUIDANCE_LAYER = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "navigation_guidance");
    private static final int COLOR = 0xFF00E5FF;
    private static final int ACTIVE_COLOR = 0xFFFFFF55;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, GUIDANCE_LAYER, NavigationGuidanceHud::render);
    }

    private static void render(GuiGraphics gg, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;
        if (mc.options.hideGui) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (!CyberpunkMinimapRenderer.hasNavigationChip(player)) return;

        MinimapWaypointClient.Waypoint waypoint = MinimapWaypointClient.getActiveWaypoint(player);

        if (waypoint == null) return;

        String dimension = player.level().dimension().location().toString();

        if (!waypoint.dimension().equals(dimension)) {
            renderDifferentDimension(gg, mc, waypoint);
            return;
        }

        double dx = waypoint.x() + 0.5D - player.getX();
        double dz = waypoint.z() + 0.5D - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relativeYaw = Mth.wrapDegrees(targetYaw - player.getYRot());
        double normalized = Mth.clamp(relativeYaw / 90.0D, -1.0D, 1.0D);

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int y = 34;
        int horizontalRange = Math.min(140, mc.getWindow().getGuiScaledWidth() / 3);
        int markerX = centerX + Mth.floor(normalized * horizontalRange);

        renderCompassLine(gg, centerX, y, horizontalRange);
        renderMarker(gg, markerX, y, relativeYaw);

        Component text = Component.translatable("gui.createcybernetics.navigation.guidance", waypointName(waypoint), Mth.floor(distance));
        gg.drawCenteredString(mc.font, text, centerX, y + 14, ACTIVE_COLOR);
    }

    private static void renderCompassLine(GuiGraphics gg, int centerX, int y, int range) {
        gg.fill(centerX - range, y, centerX + range + 1, y + 1, 0x6600E5FF);
        gg.fill(centerX, y - 3, centerX + 1, y + 4, COLOR);
        gg.fill(centerX - range, y - 2, centerX - range + 1, y + 3, COLOR);
        gg.fill(centerX + range, y - 2, centerX + range + 1, y + 3, COLOR);
    }

    private static void renderMarker(GuiGraphics gg, int x, int y, double relativeYaw) {
        if (Math.abs(relativeYaw) <= 6.0D) {
            gg.fill(x - 1, y - 7, x + 2, y + 5, ACTIVE_COLOR);
            gg.fill(x - 4, y - 4, x + 5, y - 1, ACTIVE_COLOR);
            return;
        }

        if (relativeYaw < 0.0D) {
            gg.fill(x - 5, y - 1, x + 4, y + 2, ACTIVE_COLOR);
            gg.fill(x - 5, y - 4, x - 2, y + 5, ACTIVE_COLOR);
        } else {
            gg.fill(x - 3, y - 1, x + 6, y + 2, ACTIVE_COLOR);
            gg.fill(x + 3, y - 4, x + 6, y + 5, ACTIVE_COLOR);
        }
    }

    private static void renderDifferentDimension(GuiGraphics gg, Minecraft mc, MinimapWaypointClient.Waypoint waypoint) {
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;

        gg.drawCenteredString(mc.font, waypointName(waypoint), centerX, 34, ACTIVE_COLOR);
        gg.drawCenteredString(mc.font, Component.translatable("gui.createcybernetics.navigation.different_dimension", waypoint.dimension()), centerX, 46, 0xFFFF5555);
    }

    private static Component waypointName(MinimapWaypointClient.Waypoint waypoint) {
        return MinimapWaypointClient.getWaypointDisplayName(waypoint);
    }
}