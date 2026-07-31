package com.perigrine3.createcybernetics.common.navigation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.SharedNavigationShardData;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationClearPayload;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationPlayersPayload;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationTileSyncPayload;
import com.perigrine3.createcybernetics.network.payload.SharedNavigationWaypointSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SharedNavigationServerManager {

    private SharedNavigationServerManager() {}

    private static final int PLAYER_SYNC_INTERVAL = 20;
    private static final int MAP_SYNC_INTERVAL = 100;

    private static final Map<UUID, UUID> LAST_PLAYER_NETWORKS = new HashMap<>();
    private record PlayerNetworkRevision(UUID networkId, long revision) {}
    private static final Map<UUID, PlayerNetworkRevision> LAST_SENT_REVISIONS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (server.getTickCount() % PLAYER_SYNC_INTERVAL == 0) {
            syncPlayers(server);
        }

        if (server.getTickCount() % MAP_SYNC_INTERVAL == 0) {
            syncMapData(server);
        }
    }

    private static void syncPlayers(MinecraftServer server) {
        Map<UUID, List<ServerPlayer>> networkMembers = collectNetworkMembers(server);
        Map<UUID, UUID> currentPlayerNetworks = new HashMap<>();

        for (Map.Entry<UUID, List<ServerPlayer>> entry : networkMembers.entrySet()) {
            for (ServerPlayer member : entry.getValue()) {
                currentPlayerNetworks.put(member.getUUID(), entry.getKey());
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID previousNetwork = LAST_PLAYER_NETWORKS.get(player.getUUID());
            UUID currentNetwork = currentPlayerNetworks.get(player.getUUID());

            if (currentNetwork != null && !currentNetwork.equals(previousNetwork)) {
                LAST_SENT_REVISIONS.remove(player.getUUID());
            }

            if (currentNetwork == null) {
                if (previousNetwork != null) {
                    PacketDistributor.sendToPlayer(player, new SharedNavigationClearPayload());
                }

                LAST_SENT_REVISIONS.remove(player.getUUID());
                continue;
            }

            List<ServerPlayer> members = networkMembers.get(currentNetwork);
            List<SharedNavigationPlayersPayload.SharedPlayerData> playerData = new ArrayList<>();

            for (ServerPlayer member : members) {
                playerData.add(new SharedNavigationPlayersPayload.SharedPlayerData(member.getUUID(), member.getGameProfile().getName(), member.level().dimension().location().toString(), member.getX(), member.getY(), member.getZ(), member.getYRot()));
            }

            PacketDistributor.sendToPlayer(player, new SharedNavigationPlayersPayload(currentNetwork, playerData));
        }

        LAST_SENT_REVISIONS.keySet().removeIf(playerId -> server.getPlayerList().getPlayer(playerId) == null);
        LAST_PLAYER_NETWORKS.clear();
        LAST_PLAYER_NETWORKS.putAll(currentPlayerNetworks);
    }

    private static void syncMapData(MinecraftServer server) {
        Map<UUID, List<ServerPlayer>> networkMembers = collectNetworkMembers(server);
        SharedNavigationMapData data = SharedNavigationMapData.get(server);

        for (Map.Entry<UUID, List<ServerPlayer>> entry : networkMembers.entrySet()) {
            UUID networkId = entry.getKey();
            long revision = data.getRevision(networkId);

            for (ServerPlayer player : entry.getValue()) {
                PlayerNetworkRevision lastSent = LAST_SENT_REVISIONS.get(player.getUUID());

                if (lastSent != null && lastSent.networkId().equals(networkId) && lastSent.revision() == revision) continue;

                for (SharedNavigationMapData.SharedTile tile : data.getTiles(networkId)) {
                    PacketDistributor.sendToPlayer(player, new SharedNavigationTileSyncPayload(tile.dimension(), tile.tileX(), tile.tileZ(), tile.colors().clone(), tile.explored().toLongArray()));
                }

                for (SharedNavigationMapData.SharedWaypoint waypoint : data.getWaypoints(networkId)) {
                    PacketDistributor.sendToPlayer(player, new SharedNavigationWaypointSyncPayload(waypoint.id(), waypoint.dimension(), waypoint.x(), waypoint.y(), waypoint.z(), waypoint.name(), waypoint.color(), waypoint.type()));
                }

                LAST_SENT_REVISIONS.put(player.getUUID(), new PlayerNetworkRevision(networkId, revision));
            }
        }
    }

    private static Map<UUID, List<ServerPlayer>> collectNetworkMembers(MinecraftServer server) {
        Map<UUID, List<ServerPlayer>> networkMembers = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID networkId = findActiveNetwork(player);

            if (networkId == null) continue;

            networkMembers.computeIfAbsent(networkId, ignored -> new ArrayList<>()).add(player);
        }

        return networkMembers;
    }

    public static UUID findActiveNetwork(ServerPlayer player) {
        if (player == null) return null;
        if (!hasFunctioningNavigationChip(player)) return null;

        PlayerCyberwareData cyberwareData = player.getData(ModAttachments.CYBERWARE);

        SharedNavigationMapData sharedData = SharedNavigationMapData.get(player.getServer());

        for (int slot = 0; slot < PlayerCyberwareData.CHIPWARE_SLOT_COUNT; slot++) {
            ItemStack stack = cyberwareData.getChipwareStack(slot);

            if (stack == null || stack.isEmpty()) continue;
            if (!stack.is(ModItems.DATA_SHARD_SHARED_NAVIGATION.get())) continue;

            ItemStack updated = stack.copy();
            updated.setCount(1);
            boolean changed = false;

            UUID networkId = SharedNavigationShardData.getNetworkId(updated);

            if (networkId == null) {
                networkId = SharedNavigationShardData.getOrCreateNetworkId(updated);
                changed = true;
            }

            UUID canonicalNetwork = sharedData.resolveNetworkId(networkId);

            if (!canonicalNetwork.equals(networkId)) {
                SharedNavigationShardData.setNetworkId(updated, canonicalNetwork);
                networkId = canonicalNetwork;
                changed = true;
            }

            UUID pendingMergeSource = SharedNavigationShardData.getPendingMergeSource(updated);

            if (pendingMergeSource != null) {
                sharedData.mergeNetworks(networkId, pendingMergeSource);
                canonicalNetwork = sharedData.resolveNetworkId(networkId);
                SharedNavigationShardData.setNetworkId(updated, canonicalNetwork);
                SharedNavigationShardData.clearPendingMergeSource(updated);
                networkId = canonicalNetwork;
                changed = true;
            }

            if (changed) {
                cyberwareData.setChipwareStack(slot, updated);
                cyberwareData.setDirty();
                player.syncData(ModAttachments.CYBERWARE);
            }

            return networkId;
        }

        return null;
    }

    private static boolean hasFunctioningNavigationChip(ServerPlayer player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) return false;

        InstalledCyberware[] installedEyes = data.getAll().get(CyberwareSlot.EYES);

        if (installedEyes == null) return false;

        for (int index = 0; index < installedEyes.length; index++) {
            InstalledCyberware installed = installedEyes[index];

            if (installed == null) continue;

            ItemStack stack = installed.getItem();

            if (stack == null || stack.isEmpty()) continue;
            if (!stack.is(ModItems.EYEUPGRADES_NAVIGATIONCHIP.get())) continue;
            if (!data.isEnabled(CyberwareSlot.EYES, index)) continue;

            if (stack.getItem() instanceof ICyberwareItem cyberwareItem && cyberwareItem.requiresEnergyToFunction(player, stack, CyberwareSlot.EYES) && !installed.isPowered()) continue;

            return true;
        }

        return false;
    }
}