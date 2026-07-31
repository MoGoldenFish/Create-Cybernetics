package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SharedNavigationPlayersPayload(UUID networkId, List<SharedPlayerData> players) implements CustomPacketPayload {

    public static final Type<SharedNavigationPlayersPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shared_navigation_players"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SharedNavigationPlayersPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.networkId());
                buffer.writeVarInt(payload.players().size());

                for (SharedPlayerData player : payload.players()) {
                    SharedPlayerData.STREAM_CODEC.encode(buffer, player);
                }
            },
            buffer -> {
                UUID networkId = buffer.readUUID();
                int playerCount = buffer.readVarInt();
                List<SharedPlayerData> players = new ArrayList<>(playerCount);

                for (int index = 0; index < playerCount; index++) {
                    players.add(SharedPlayerData.STREAM_CODEC.decode(buffer));
                }

                return new SharedNavigationPlayersPayload(networkId, players);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedNavigationPlayersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) return;

            List<SharedNavigationClientState.SharedPlayer> players = new ArrayList<>(payload.players().size());

            for (SharedPlayerData player : payload.players()) {
                players.add(new SharedNavigationClientState.SharedPlayer(player.playerId(), player.name(), player.dimension(), player.x(), player.y(), player.z(), player.yaw()));
            }

            SharedNavigationClientState.acceptPlayers(payload.networkId(), players);
        });
    }

    public record SharedPlayerData(UUID playerId, String name, String dimension, double x, double y, double z, float yaw) {

        private static final StreamCodec<RegistryFriendlyByteBuf, SharedPlayerData> STREAM_CODEC = StreamCodec.of(
                (buffer, player) -> {
                    buffer.writeUUID(player.playerId());
                    buffer.writeUtf(player.name());
                    buffer.writeUtf(player.dimension());
                    buffer.writeDouble(player.x());
                    buffer.writeDouble(player.y());
                    buffer.writeDouble(player.z());
                    buffer.writeFloat(player.yaw());
                },
                buffer -> new SharedPlayerData(buffer.readUUID(), buffer.readUtf(), buffer.readUtf(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat())
        );
    }
}