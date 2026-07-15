package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChessRequestSyncPayload(
        BlockPos computerPos
) implements CustomPacketPayload {

    public static final Type<ChessRequestSyncPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_request_sync"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessRequestSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) ->
                            buffer.writeBlockPos(
                                    payload.computerPos()
                            ),
                    buffer ->
                            new ChessRequestSyncPayload(
                                    buffer.readBlockPos()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}