package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SharedNavigationWaypointSyncPayload(String id, String dimension, int x, int y, int z, String name, int color, String waypointType) implements CustomPacketPayload {

    public static final Type<SharedNavigationWaypointSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shared_navigation_waypoint_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SharedNavigationWaypointSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.id());
                buffer.writeUtf(payload.dimension());
                buffer.writeInt(payload.x());
                buffer.writeInt(payload.y());
                buffer.writeInt(payload.z());
                buffer.writeUtf(payload.name());
                buffer.writeInt(payload.color());
                buffer.writeUtf(payload.waypointType());
            },
            buffer -> new SharedNavigationWaypointSyncPayload(buffer.readUtf(), buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUtf(), buffer.readInt(), buffer.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedNavigationWaypointSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            SharedNavigationClientState.mergeWaypoint(payload.id(), payload.dimension(), payload.x(), payload.y(), payload.z(), payload.name(), payload.color(), payload.waypointType());
        });
    }
}