package com.perigrine3.createcybernetics.client.biomonitor;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.network.payload.BiomonitorScanRequestPayload;
import com.perigrine3.createcybernetics.network.payload.BiomonitorVitalsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = CreateCybernetics.MODID,
        value = Dist.CLIENT
)
public final class BiomonitorClientEvents {
    private BiomonitorClientEvents() {
    }

    private static final double BIOMONITOR_RANGE = 96.0D;

    /*
     * A half-second refresh rate is responsive enough for effect durations,
     * health changes, hunger changes, and status changes without sending one
     * request per render frame.
     */
    private static final int SCAN_REFRESH_INTERVAL_TICKS = 10;

    private static int lastRequestedTargetId = -1;
    private static int lastRequestTick = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) {
            resetClientScanState();
            return;
        }

        if (!hasBiomonitorInstalled(player)) {
            resetClientScanState();
            return;
        }

        LivingEntity target = BiomonitorTargeting.findLookedAtLivingEntity(
                player,
                BIOMONITOR_RANGE,
                1.0F
        );

        if (target == null) {
            resetClientScanState();
            return;
        }

        int targetId = target.getId();
        int currentTick = player.tickCount;

        boolean targetChanged = targetId != lastRequestedTargetId;
        boolean refreshDue = currentTick - lastRequestTick
                >= SCAN_REFRESH_INTERVAL_TICKS;

        if (!targetChanged && !refreshDue) {
            return;
        }

        PacketDistributor.sendToServer(
                new BiomonitorScanRequestPayload(targetId)
        );

        lastRequestedTargetId = targetId;
        lastRequestTick = currentTick;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) {
            return;
        }

        if (!hasBiomonitorInstalled(player)) {
            return;
        }

        float partialTick = event.getPartialTick()
                .getGameTimeDeltaPartialTick(false);

        LivingEntity target = BiomonitorTargeting.findLookedAtLivingEntity(
                player,
                BIOMONITOR_RANGE,
                partialTick
        );

        if (target == null) {
            return;
        }

        BiomonitorVitalsPayload snapshot =
                BiomonitorClientData.getForTarget(target.getId());

        BiomonitorRenderer.render(
                event.getPoseStack(),
                event.getCamera(),
                target,
                snapshot,
                partialTick
        );
    }

    private static boolean hasBiomonitorInstalled(LocalPlayer player) {
        PlayerCyberwareData cyberwareData = player.getData(ModAttachments.CYBERWARE);

        return cyberwareData.hasSpecificItem(ModItems.EYEUPGRADES_BIOMONITOR.get(), CyberwareSlot.EYES);
    }

    private static void resetClientScanState() {
        lastRequestedTargetId = -1;
        lastRequestTick = Integer.MIN_VALUE;

        BiomonitorClientData.clear();
    }
}