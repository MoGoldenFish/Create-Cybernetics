package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientState;
import com.perigrine3.createcybernetics.common.navigation.SharedNavigationMapData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SharedNavigationTileSyncPayload(String dimension, int tileX, int tileZ, int[] colors, long[] explored) implements CustomPacketPayload {

    public static final Type<SharedNavigationTileSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shared_navigation_tile_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SharedNavigationTileSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.dimension());
                buffer.writeInt(payload.tileX());
                buffer.writeInt(payload.tileZ());
                buffer.writeVarInt(payload.colors().length);

                for (int color : payload.colors()) {
                    buffer.writeInt(color);
                }

                buffer.writeVarInt(payload.explored().length);

                for (long word : payload.explored()) {
                    buffer.writeLong(word);
                }
            },
            buffer -> {
                String dimension = buffer.readUtf();
                int tileX = buffer.readInt();
                int tileZ = buffer.readInt();
                int colorCount = buffer.readVarInt();

                if (colorCount < 0 || colorCount > SharedNavigationMapData.TILE_PIXELS) {
                    throw new IllegalArgumentException("Invalid shared navigation tile color count");
                }

                int[] colors = new int[colorCount];

                for (int index = 0; index < colorCount; index++) {
                    colors[index] = buffer.readInt();
                }

                int exploredCount = buffer.readVarInt();

                if (exploredCount < 0 || exploredCount > 64) {
                    throw new IllegalArgumentException("Invalid shared navigation explored word count");
                }

                long[] explored = new long[exploredCount];

                for (int index = 0; index < exploredCount; index++) {
                    explored[index] = buffer.readLong();
                }

                return new SharedNavigationTileSyncPayload(dimension, tileX, tileZ, colors, explored);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedNavigationTileSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            SharedNavigationClientState.mergeTile(payload.dimension(), payload.tileX(), payload.tileZ(), payload.colors(), payload.explored());
        });
    }
}