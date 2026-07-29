package com.perigrine3.createcybernetics.client.navigation;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationTileUploadPayload;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationWaypointUploadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SharedNavigationClientSync {

    private SharedNavigationClientSync() {}

    private static final int REBUILD_INTERVAL = 200;

    private static final Queue<MinimapWaypointClient.NavigationTileSnapshot> TILE_QUEUE = new ArrayDeque<>();
    private static final Queue<MinimapWaypointClient.NavigationWaypointSnapshot> WAYPOINT_QUEUE = new ArrayDeque<>();
    private static final Map<String, Integer> SENT_TILE_HASHES = new HashMap<>();
    private static final Map<String, Integer> SENT_WAYPOINT_HASHES = new HashMap<>();

    private static UUID queuedNetworkId;
    private static int lastRebuildTick = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        UUID networkId = SharedNavigationClientState.getNetworkId();

        if (player == null || networkId == null || minecraft.getConnection() == null) {
            clearQueues();
            return;
        }

        if (!networkId.equals(queuedNetworkId)) {
            clearQueues();
            queuedNetworkId = networkId;
        }

        if (lastRebuildTick == Integer.MIN_VALUE || player.tickCount - lastRebuildTick >= REBUILD_INTERVAL) {
            rebuildQueues(player);
            lastRebuildTick = player.tickCount;
        }

        MinimapWaypointClient.NavigationTileSnapshot tile = TILE_QUEUE.poll();

        if (tile != null) {
            PacketDistributor.sendToServer(new SharedNavigationTileUploadPayload(networkId, tile.dimension(), tile.tileX(), tile.tileZ(), tile.colors(), tile.explored()));
            return;
        }

        MinimapWaypointClient.NavigationWaypointSnapshot waypoint = WAYPOINT_QUEUE.poll();

        if (waypoint != null) {
            PacketDistributor.sendToServer(new SharedNavigationWaypointUploadPayload(networkId, waypoint.id(), waypoint.dimension(), waypoint.x(), waypoint.y(), waypoint.z(), waypoint.name(), waypoint.color(), waypoint.type()));
        }
    }

    private static void rebuildQueues(LocalPlayer player) {
        for (MinimapWaypointClient.NavigationTileSnapshot tile : MinimapWaypointClient.createTileSnapshot(player)) {
            String key = tile.dimension() + "|" + tile.tileX() + "|" + tile.tileZ();
            int hash = 31 * Arrays.hashCode(tile.colors()) + Arrays.hashCode(tile.explored());
            Integer previousHash = SENT_TILE_HASHES.put(key, hash);

            if (previousHash != null && previousHash == hash) continue;

            TILE_QUEUE.add(tile);
        }

        for (MinimapWaypointClient.NavigationWaypointSnapshot waypoint : MinimapWaypointClient.createWaypointSnapshot(player)) {
            int hash = waypoint.hashCode();
            Integer previousHash = SENT_WAYPOINT_HASHES.put(waypoint.id(), hash);

            if (previousHash != null && previousHash == hash) continue;

            WAYPOINT_QUEUE.add(waypoint);
        }
    }

    public static void clearQueues() {
        TILE_QUEUE.clear();
        WAYPOINT_QUEUE.clear();
        SENT_TILE_HASHES.clear();
        SENT_WAYPOINT_HASHES.clear();
        queuedNetworkId = null;
        lastRebuildTick = Integer.MIN_VALUE;
    }
}