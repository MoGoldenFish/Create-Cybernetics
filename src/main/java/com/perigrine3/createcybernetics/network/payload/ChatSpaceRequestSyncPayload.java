package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatSpaceRequestSyncPayload(
        BlockPos computerPos
) implements CustomPacketPayload {
    public static final Type<ChatSpaceRequestSyncPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chatspace_request_sync"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatSpaceRequestSyncPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) ->
                            buffer.writeBlockPos(
                                    payload.computerPos
                            ),
                    buffer ->
                            new ChatSpaceRequestSyncPayload(
                                    buffer.readBlockPos()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}